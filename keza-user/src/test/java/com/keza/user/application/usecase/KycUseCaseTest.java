package com.keza.user.application.usecase;

import com.keza.common.enums.DocumentType;
import com.keza.common.enums.KycStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ForbiddenException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.AuditLogger;
import com.keza.infrastructure.config.StorageConfig;
import com.keza.infrastructure.config.StorageService;
import com.keza.user.application.dto.KycDocumentResponse;
import com.keza.user.domain.event.KycStatusChangedEvent;
import com.keza.user.domain.model.KycDocument;
import com.keza.user.domain.model.KycDocumentStatus;
import com.keza.user.domain.model.User;
import com.keza.user.domain.model.UserRole;
import com.keza.user.domain.port.out.KycDocumentRepository;
import com.keza.user.domain.port.out.UserRepository;
import com.keza.user.domain.service.KycStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycUseCase")
class KycUseCaseTest {

    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private StorageConfig storageConfig;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private KycStateMachine kycStateMachine;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditLogger auditLogger;

    @InjectMocks
    private KycUseCase kycUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID REVIEWER_ID = UUID.randomUUID();

    private User testUser;
    private KycDocument testDocument;

    @BeforeEach
    void setUp() {
        UserRole role = UserRole.builder().name("INVESTOR").build();
        role.setId(UUID.randomUUID());

        testUser = User.builder()
                .email("jane@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .roles(Set.of(role))
                .build();
        testUser.setId(USER_ID);
        testUser.setCreatedAt(Instant.now());

        testDocument = KycDocument.builder()
                .userId(USER_ID)
                .documentType(DocumentType.NATIONAL_ID)
                .fileKey("kyc/" + USER_ID + "/national_id/test.jpg")
                .fileName("id_card.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .status(KycDocumentStatus.PENDING)
                .build();
        testDocument.setId(DOCUMENT_ID);
        testDocument.setCreatedAt(Instant.now());
        testDocument.setUpdatedAt(Instant.now());
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file", "id_card.jpg", "image/jpeg", new byte[1024]);
    }

    private void stubBucketConfig() {
        when(storageConfig.getBuckets()).thenReturn(Map.of("kyc", "keza-kyc-bucket"));
    }

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocument {

        @Test
        @DisplayName("should upload document successfully and return response")
        void shouldUploadSuccessfully() throws IOException {
            MockMultipartFile file = validFile();
            stubBucketConfig();
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(storageService.upload(anyString(), anyString(), any(), anyLong(), anyString())).thenReturn("key");
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);

            KycDocumentResponse response = kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, file);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(DOCUMENT_ID);
            assertThat(response.getDocumentType()).isEqualTo("NATIONAL_ID");
            assertThat(response.getStatus()).isEqualTo("PENDING");
            verify(storageService).upload(eq("keza-kyc-bucket"), anyString(), any(), eq(1024L), eq("image/jpeg"));
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(DOCUMENT_ID));
            verify(auditLogger).log(eq("KYC_DOCUMENT_UPLOADED"), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should update user KYC status to SUBMITTED when currently PENDING")
        void shouldUpdateUserKycStatusToSubmitted() {
            testUser.setKycStatus(KycStatus.PENDING);
            MockMultipartFile file = validFile();
            stubBucketConfig();
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(storageService.upload(anyString(), anyString(), any(), anyLong(), anyString())).thenReturn("key");
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);

            kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, file);

            assertThat(testUser.getKycStatus()).isEqualTo(KycStatus.SUBMITTED);
            verify(userRepository).save(testUser);
            verify(eventPublisher).publishEvent(any(KycStatusChangedEvent.class));
        }

        @Test
        @DisplayName("should not change user KYC status when not PENDING")
        void shouldNotChangeKycStatusWhenNotPending() {
            testUser.setKycStatus(KycStatus.SUBMITTED);
            MockMultipartFile file = validFile();
            stubBucketConfig();
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(storageService.upload(anyString(), anyString(), any(), anyLong(), anyString())).thenReturn("key");
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);

            kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, file);

            assertThat(testUser.getKycStatus()).isEqualTo(KycStatus.SUBMITTED);
            // userRepository.save is not called for user status change, only for doc
            verify(eventPublisher, never()).publishEvent(any(KycStatusChangedEvent.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, validFile()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BusinessRuleException when file is empty")
        void shouldThrowWhenFileIsEmpty() {
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, emptyFile))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when file exceeds 10MB")
        void shouldThrowWhenFileTooLarge() {
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
            MockMultipartFile largeFile = new MockMultipartFile("file", "big.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, largeFile))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("exceeds");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when content type is not allowed")
        void shouldThrowWhenInvalidContentType() {
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            MockMultipartFile gifFile = new MockMultipartFile("file", "anim.gif", "image/gif", new byte[100]);

            assertThatThrownBy(() -> kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, gifFile))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when S3 upload fails")
        void shouldThrowWhenUploadFails() {
            stubBucketConfig();
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));

            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getOriginalFilename()).thenReturn("id.jpg");
            try {
                when(mockFile.getInputStream()).thenThrow(new IOException("disk error"));
            } catch (IOException e) {
                // won't happen
            }

            assertThatThrownBy(() -> kycUseCase.uploadDocument(USER_ID, DocumentType.NATIONAL_ID, mockFile))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Failed to upload document");
        }
    }

    @Nested
    @DisplayName("getDocuments")
    class GetDocuments {

        @Test
        @DisplayName("should return list of document responses for a user")
        void shouldReturnDocuments() {
            when(kycDocumentRepository.findByUserId(USER_ID)).thenReturn(List.of(testDocument));

            List<KycDocumentResponse> docs = kycUseCase.getDocuments(USER_ID);

            assertThat(docs).hasSize(1);
            assertThat(docs.get(0).getId()).isEqualTo(DOCUMENT_ID);
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void shouldReturnEmptyList() {
            when(kycDocumentRepository.findByUserId(USER_ID)).thenReturn(List.of());

            List<KycDocumentResponse> docs = kycUseCase.getDocuments(USER_ID);

            assertThat(docs).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDocumentUrl")
    class GetDocumentUrl {

        @Test
        @DisplayName("should return presigned URL for the document owner")
        void shouldReturnPresignedUrl() {
            stubBucketConfig();
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
            when(storageService.generatePresignedUrl(anyString(), anyString(), any())).thenReturn("https://presigned.url");

            String url = kycUseCase.getDocumentUrl(USER_ID, DOCUMENT_ID);

            assertThat(url).isEqualTo("https://presigned.url");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user does not own the document")
        void shouldThrowWhenNotOwner() {
            UUID otherUserId = UUID.randomUUID();
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));

            assertThatThrownBy(() -> kycUseCase.getDocumentUrl(otherUserId, DOCUMENT_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when document not found")
        void shouldThrowWhenDocumentNotFound() {
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kycUseCase.getDocumentUrl(USER_ID, DOCUMENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("adminReviewDocument")
    class AdminReviewDocument {

        @Test
        @DisplayName("should approve a PENDING document, transitioning through IN_REVIEW")
        void shouldApprovePendingDocument() {
            testDocument.setStatus(KycDocumentStatus.PENDING);
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);
            // After approval, checkAndUpdateUserKycStatus will be called
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(kycStateMachine.getRequiredDocumentTypes()).thenReturn(Set.of(DocumentType.NATIONAL_ID, DocumentType.SELFIE));
            when(kycDocumentRepository.findByUserIdAndDocumentType(eq(USER_ID), any())).thenReturn(List.of());

            KycDocumentResponse response = kycUseCase.adminReviewDocument(DOCUMENT_ID, REVIEWER_ID, true, null);

            verify(kycStateMachine).validateTransition(KycDocumentStatus.PENDING, KycDocumentStatus.IN_REVIEW);
            verify(kycStateMachine).validateTransition(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.APPROVED);
            assertThat(testDocument.getStatus()).isEqualTo(KycDocumentStatus.APPROVED);
            assertThat(testDocument.getReviewedBy()).isEqualTo(REVIEWER_ID);
            assertThat(testDocument.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject a PENDING document with reason")
        void shouldRejectPendingDocumentWithReason() {
            testDocument.setStatus(KycDocumentStatus.PENDING);
            testUser.setKycStatus(KycStatus.SUBMITTED);
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));

            KycDocumentResponse response = kycUseCase.adminReviewDocument(DOCUMENT_ID, REVIEWER_ID, false, "Blurry image");

            assertThat(testDocument.getStatus()).isEqualTo(KycDocumentStatus.REJECTED);
            assertThat(testDocument.getRejectionReason()).isEqualTo("Blurry image");
            assertThat(testUser.getKycStatus()).isEqualTo(KycStatus.REJECTED);
            verify(eventPublisher).publishEvent(any(KycStatusChangedEvent.class));
        }

        @Test
        @DisplayName("should approve an IN_REVIEW document directly")
        void shouldApproveInReviewDocument() {
            testDocument.setStatus(KycDocumentStatus.IN_REVIEW);
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(kycStateMachine.getRequiredDocumentTypes()).thenReturn(Set.of(DocumentType.NATIONAL_ID, DocumentType.SELFIE));
            when(kycDocumentRepository.findByUserIdAndDocumentType(eq(USER_ID), any())).thenReturn(List.of());

            kycUseCase.adminReviewDocument(DOCUMENT_ID, REVIEWER_ID, true, null);

            // Should NOT validate PENDING -> IN_REVIEW transition, only IN_REVIEW -> APPROVED
            verify(kycStateMachine, times(1)).validateTransition(any(), any());
            verify(kycStateMachine).validateTransition(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.APPROVED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when document not found")
        void shouldThrowWhenDocumentNotFound() {
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kycUseCase.adminReviewDocument(DOCUMENT_ID, REVIEWER_ID, true, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should not set rejection reason when approved")
        void shouldNotSetRejectionReasonWhenApproved() {
            testDocument.setStatus(KycDocumentStatus.IN_REVIEW);
            when(kycDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testDocument);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(kycStateMachine.getRequiredDocumentTypes()).thenReturn(Set.of(DocumentType.NATIONAL_ID, DocumentType.SELFIE));
            when(kycDocumentRepository.findByUserIdAndDocumentType(eq(USER_ID), any())).thenReturn(List.of());

            kycUseCase.adminReviewDocument(DOCUMENT_ID, REVIEWER_ID, true, "some reason");

            assertThat(testDocument.getRejectionReason()).isNull();
        }
    }

    @Nested
    @DisplayName("checkAndUpdateUserKycStatus")
    class CheckAndUpdateUserKycStatus {

        @Test
        @DisplayName("should auto-approve user KYC when all required documents are approved")
        void shouldAutoApproveWhenAllDocsApproved() {
            testUser.setKycStatus(KycStatus.SUBMITTED);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(kycStateMachine.getRequiredDocumentTypes())
                    .thenReturn(Set.of(DocumentType.NATIONAL_ID, DocumentType.SELFIE));

            KycDocument approvedNationalId = KycDocument.builder()
                    .status(KycDocumentStatus.APPROVED).build();
            KycDocument approvedSelfie = KycDocument.builder()
                    .status(KycDocumentStatus.APPROVED).build();

            when(kycDocumentRepository.findByUserIdAndDocumentType(USER_ID, DocumentType.NATIONAL_ID))
                    .thenReturn(List.of(approvedNationalId));
            when(kycDocumentRepository.findByUserIdAndDocumentType(USER_ID, DocumentType.SELFIE))
                    .thenReturn(List.of(approvedSelfie));

            kycUseCase.checkAndUpdateUserKycStatus(USER_ID);

            assertThat(testUser.getKycStatus()).isEqualTo(KycStatus.APPROVED);
            verify(userRepository).save(testUser);
            verify(eventPublisher).publishEvent(any(KycStatusChangedEvent.class));
            verify(auditLogger).log(eq("KYC_AUTO_APPROVED"), anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should not auto-approve when some required documents are missing")
        void shouldNotAutoApproveWhenDocsMissing() {
            testUser.setKycStatus(KycStatus.SUBMITTED);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(kycStateMachine.getRequiredDocumentTypes())
                    .thenReturn(Set.of(DocumentType.NATIONAL_ID, DocumentType.SELFIE));

            KycDocument approvedNationalId = KycDocument.builder()
                    .status(KycDocumentStatus.APPROVED).build();

            lenient().when(kycDocumentRepository.findByUserIdAndDocumentType(USER_ID, DocumentType.NATIONAL_ID))
                    .thenReturn(List.of(approvedNationalId));
            lenient().when(kycDocumentRepository.findByUserIdAndDocumentType(USER_ID, DocumentType.SELFIE))
                    .thenReturn(List.of()); // no selfie

            kycUseCase.checkAndUpdateUserKycStatus(USER_ID);

            assertThat(testUser.getKycStatus()).isEqualTo(KycStatus.SUBMITTED); // unchanged
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not auto-approve when already APPROVED")
        void shouldNotAutoApproveWhenAlreadyApproved() {
            testUser.setKycStatus(KycStatus.APPROVED);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(kycStateMachine.getRequiredDocumentTypes())
                    .thenReturn(Set.of(DocumentType.NATIONAL_ID, DocumentType.SELFIE));

            KycDocument approved = KycDocument.builder().status(KycDocumentStatus.APPROVED).build();
            when(kycDocumentRepository.findByUserIdAndDocumentType(eq(USER_ID), any()))
                    .thenReturn(List.of(approved));

            kycUseCase.checkAndUpdateUserKycStatus(USER_ID);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("getPendingDocuments")
    class GetPendingDocuments {

        @Test
        @DisplayName("should return all pending documents")
        void shouldReturnPendingDocuments() {
            when(kycDocumentRepository.findByStatus(KycDocumentStatus.PENDING))
                    .thenReturn(List.of(testDocument));

            List<KycDocumentResponse> result = kycUseCase.getPendingDocuments();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        }
    }
}
