package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.CampaignRequest;
import com.keza.campaign.application.dto.CampaignResponse;
import com.keza.campaign.application.dto.CampaignSearchCriteria;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.CampaignMedia;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.service.CampaignStateMachine;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.OfferingType;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignUseCase")
class CampaignUseCaseTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignStateMachine stateMachine;

    @InjectMocks
    private CampaignUseCase campaignUseCase;

    @Captor
    private ArgumentCaptor<Campaign> campaignCaptor;

    private UUID issuerId;
    private UUID campaignId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        issuerId = UUID.randomUUID();
        campaignId = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    private Campaign buildDraftCampaign() {
        Campaign campaign = Campaign.builder()
                .issuerId(issuerId)
                .title("Untitled Campaign")
                .targetAmount(BigDecimal.ZERO)
                .status(CampaignStatus.DRAFT)
                .wizardStep(1)
                .build();
        campaign.setId(campaignId);
        return campaign;
    }

    private Campaign buildCompleteCampaign() {
        Campaign campaign = Campaign.builder()
                .issuerId(issuerId)
                .title("Green Energy Fund")
                .slug("green-energy-fund-" + campaignId.toString().substring(0, 8))
                .tagline("Invest in a greener future")
                .description("A comprehensive green energy investment opportunity")
                .industry("Renewable Energy")
                .companyName("GreenTech Ltd")
                .companyRegistrationNumber("REG-12345")
                .companyWebsite("https://greentech.example.com")
                .companyAddress("123 Green St")
                .offeringType(OfferingType.EQUITY)
                .targetAmount(new BigDecimal("500000"))
                .sharePrice(new BigDecimal("10.00"))
                .totalShares(50000L)
                .minInvestment(new BigDecimal("1000"))
                .maxInvestment(new BigDecimal("50000"))
                .status(CampaignStatus.DRAFT)
                .wizardStep(6)
                .endDate(Instant.now().plus(90, ChronoUnit.DAYS))
                .build();
        campaign.setId(campaignId);
        return campaign;
    }

    @Nested
    @DisplayName("createDraft")
    class CreateDraft {

        @Test
        @DisplayName("should create a draft campaign with default values")
        void shouldCreateDraftWithDefaults() {
            Campaign saved = buildDraftCampaign();
            when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

            CampaignResponse response = campaignUseCase.createDraft(issuerId);

            verify(campaignRepository).save(campaignCaptor.capture());
            Campaign captured = campaignCaptor.getValue();

            assertThat(captured.getIssuerId()).isEqualTo(issuerId);
            assertThat(captured.getTitle()).isEqualTo("Untitled Campaign");
            assertThat(captured.getTargetAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(captured.getStatus()).isEqualTo(CampaignStatus.DRAFT);
            assertThat(captured.getWizardStep()).isEqualTo(1);

            assertThat(response).isNotNull();
            assertThat(response.getIssuerId()).isEqualTo(issuerId);
            assertThat(response.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        }

        @Test
        @DisplayName("should return a properly mapped response")
        void shouldReturnMappedResponse() {
            Campaign saved = buildDraftCampaign();
            when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

            CampaignResponse response = campaignUseCase.createDraft(issuerId);

            assertThat(response.getId()).isEqualTo(campaignId);
            assertThat(response.getTitle()).isEqualTo("Untitled Campaign");
            assertThat(response.getWizardStep()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateWizardStep")
    class UpdateWizardStep {

        @Test
        @DisplayName("step 1 - should apply company info")
        void shouldApplyCompanyInfo() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.CompanyInfoRequest request = CampaignRequest.CompanyInfoRequest.builder()
                    .companyName("Test Corp")
                    .registrationNumber("REG-001")
                    .website("https://test.com")
                    .address("123 Test St")
                    .industry("Technology")
                    .build();

            CampaignResponse response = campaignUseCase.updateWizardStep(campaignId, 1, request);

            assertThat(response.getCompanyName()).isEqualTo("Test Corp");
            assertThat(response.getIndustry()).isEqualTo("Technology");
        }

        @Test
        @DisplayName("step 2 - should apply offering details")
        void shouldApplyOfferingDetails() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            Instant endDate = Instant.now().plus(90, ChronoUnit.DAYS);
            CampaignRequest.OfferingDetailsRequest request = CampaignRequest.OfferingDetailsRequest.builder()
                    .offeringType(OfferingType.EQUITY)
                    .targetAmount(new BigDecimal("500000"))
                    .sharePrice(new BigDecimal("10.00"))
                    .totalShares(50000L)
                    .minInvestment(new BigDecimal("1000"))
                    .maxInvestment(new BigDecimal("50000"))
                    .endDate(endDate)
                    .build();

            CampaignResponse response = campaignUseCase.updateWizardStep(campaignId, 2, request);

            assertThat(response.getTargetAmount()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(response.getSharePrice()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(response.getTotalShares()).isEqualTo(50000L);
            assertThat(response.getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("step 3 - should apply pitch content and generate slug")
        void shouldApplyPitchContent() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.PitchContentRequest request = CampaignRequest.PitchContentRequest.builder()
                    .title("Green Energy Fund")
                    .tagline("Invest in green")
                    .description("A great opportunity")
                    .pitchVideoUrl("https://video.example.com/pitch")
                    .build();

            CampaignResponse response = campaignUseCase.updateWizardStep(campaignId, 3, request);

            assertThat(response.getTitle()).isEqualTo("Green Energy Fund");
            assertThat(response.getTagline()).isEqualTo("Invest in green");
            assertThat(response.getDescription()).isEqualTo("A great opportunity");
            assertThat(response.getSlug()).startsWith("green-energy-fund-");
        }

        @Test
        @DisplayName("step 4 - should apply financial projections")
        void shouldApplyFinancialProjections() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.FinancialProjectionsRequest request = CampaignRequest.FinancialProjectionsRequest.builder()
                    .financialProjections("{\"year1\": 100000}")
                    .useOfFunds("{\"marketing\": 50}")
                    .riskFactors("Market risk, regulatory risk")
                    .build();

            CampaignResponse response = campaignUseCase.updateWizardStep(campaignId, 4, request);

            assertThat(response.getFinancialProjections()).isEqualTo("{\"year1\": 100000}");
            assertThat(response.getUseOfFunds()).isEqualTo("{\"marketing\": 50}");
            assertThat(response.getRiskFactors()).isEqualTo("Market risk, regulatory risk");
        }

        @Test
        @DisplayName("step 5 - should accept acknowledged documents")
        void shouldAcceptAcknowledgedDocuments() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.DocumentsRequest request = CampaignRequest.DocumentsRequest.builder()
                    .acknowledged(true)
                    .build();

            CampaignResponse response = campaignUseCase.updateWizardStep(campaignId, 5, request);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("step 5 - should throw when documents not acknowledged")
        void shouldThrowWhenDocumentsNotAcknowledged() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            CampaignRequest.DocumentsRequest request = CampaignRequest.DocumentsRequest.builder()
                    .acknowledged(false)
                    .build();

            assertThatThrownBy(() -> campaignUseCase.updateWizardStep(campaignId, 5, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("documents have been uploaded");
        }

        @Test
        @DisplayName("step 6 - should accept confirmed review submission")
        void shouldAcceptConfirmedReviewSubmission() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.ReviewSubmitRequest request = CampaignRequest.ReviewSubmitRequest.builder()
                    .confirmed(true)
                    .build();

            CampaignResponse response = campaignUseCase.updateWizardStep(campaignId, 6, request);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("step 6 - should throw when submission not confirmed")
        void shouldThrowWhenSubmissionNotConfirmed() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            CampaignRequest.ReviewSubmitRequest request = CampaignRequest.ReviewSubmitRequest.builder()
                    .confirmed(false)
                    .build();

            assertThatThrownBy(() -> campaignUseCase.updateWizardStep(campaignId, 6, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("confirm before submitting");
        }

        @Test
        @DisplayName("should throw for invalid wizard step number")
        void shouldThrowForInvalidStep() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.updateWizardStep(campaignId, 7, new Object()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid wizard step: 7");
        }

        @Test
        @DisplayName("should throw for step 0")
        void shouldThrowForStepZero() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.updateWizardStep(campaignId, 0, new Object()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid wizard step: 0");
        }

        @Test
        @DisplayName("should throw when campaign is not in DRAFT status")
        void shouldThrowWhenNotDraft() {
            Campaign campaign = buildDraftCampaign();
            campaign.setStatus(CampaignStatus.REVIEW);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            CampaignRequest.CompanyInfoRequest request = CampaignRequest.CompanyInfoRequest.builder()
                    .companyName("Test Corp")
                    .build();

            assertThatThrownBy(() -> campaignUseCase.updateWizardStep(campaignId, 1, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("DRAFT status");
        }

        @Test
        @DisplayName("should advance wizard step when step is higher than current")
        void shouldAdvanceWizardStep() {
            Campaign campaign = buildDraftCampaign();
            campaign.setWizardStep(1);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.OfferingDetailsRequest request = CampaignRequest.OfferingDetailsRequest.builder()
                    .offeringType(OfferingType.EQUITY)
                    .targetAmount(new BigDecimal("100000"))
                    .minInvestment(new BigDecimal("500"))
                    .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();

            campaignUseCase.updateWizardStep(campaignId, 2, request);

            verify(campaignRepository).save(campaignCaptor.capture());
            assertThat(campaignCaptor.getValue().getWizardStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not regress wizard step when revisiting earlier step")
        void shouldNotRegressWizardStep() {
            Campaign campaign = buildDraftCampaign();
            campaign.setWizardStep(4);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignRequest.CompanyInfoRequest request = CampaignRequest.CompanyInfoRequest.builder()
                    .companyName("Updated Corp")
                    .build();

            campaignUseCase.updateWizardStep(campaignId, 1, request);

            verify(campaignRepository).save(campaignCaptor.capture());
            assertThat(campaignCaptor.getValue().getWizardStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("should throw when campaign not found")
        void shouldThrowWhenCampaignNotFound() {
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            CampaignRequest.CompanyInfoRequest request = CampaignRequest.CompanyInfoRequest.builder()
                    .companyName("Test Corp")
                    .build();

            assertThatThrownBy(() -> campaignUseCase.updateWizardStep(campaignId, 1, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCampaign")
    class GetCampaign {

        @Test
        @DisplayName("should return campaign by id")
        void shouldReturnCampaignById() {
            Campaign campaign = buildCompleteCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            CampaignResponse response = campaignUseCase.getCampaign(campaignId);

            assertThat(response.getId()).isEqualTo(campaignId);
            assertThat(response.getTitle()).isEqualTo("Green Energy Fund");
            assertThat(response.getCompanyName()).isEqualTo("GreenTech Ltd");
        }

        @Test
        @DisplayName("should throw when campaign not found by id")
        void shouldThrowWhenNotFound() {
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignUseCase.getCampaign(campaignId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should map media to response when present")
        void shouldMapMediaToResponse() {
            Campaign campaign = buildCompleteCampaign();
            CampaignMedia media = CampaignMedia.builder()
                    .campaignId(campaignId)
                    .fileKey("uploads/image.jpg")
                    .fileName("image.jpg")
                    .fileSize(1024L)
                    .contentType("image/jpeg")
                    .mediaType(CampaignMedia.MediaType.IMAGE)
                    .sortOrder(0)
                    .build();
            media.setId(UUID.randomUUID());
            campaign.setMedia(List.of(media));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            CampaignResponse response = campaignUseCase.getCampaign(campaignId);

            assertThat(response.getMedia()).hasSize(1);
            assertThat(response.getMedia().get(0).getFileName()).isEqualTo("image.jpg");
            assertThat(response.getMedia().get(0).getMediaType()).isEqualTo("IMAGE");
        }

        @Test
        @DisplayName("should handle null media list gracefully")
        void shouldHandleNullMedia() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setMedia(null);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            CampaignResponse response = campaignUseCase.getCampaign(campaignId);

            assertThat(response.getMedia()).isNull();
        }
    }

    @Nested
    @DisplayName("getCampaignBySlug")
    class GetCampaignBySlug {

        @Test
        @DisplayName("should return campaign by slug")
        void shouldReturnCampaignBySlug() {
            Campaign campaign = buildCompleteCampaign();
            String slug = campaign.getSlug();
            when(campaignRepository.findBySlug(slug)).thenReturn(Optional.of(campaign));

            CampaignResponse response = campaignUseCase.getCampaignBySlug(slug);

            assertThat(response.getSlug()).isEqualTo(slug);
            assertThat(response.getTitle()).isEqualTo("Green Energy Fund");
        }

        @Test
        @DisplayName("should throw when campaign not found by slug")
        void shouldThrowWhenSlugNotFound() {
            when(campaignRepository.findBySlug("nonexistent-slug")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignUseCase.getCampaignBySlug("nonexistent-slug"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchCampaigns")
    class SearchCampaigns {

        @Test
        @DisplayName("should return paged results")
        void shouldReturnPagedResults() {
            Campaign campaign = buildCompleteCampaign();
            Page<Campaign> page = new PageImpl<>(List.of(campaign), PageRequest.of(0, 20), 1);
            when(campaignRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            CampaignSearchCriteria criteria = CampaignSearchCriteria.builder().build();
            Pageable pageable = PageRequest.of(0, 20);

            Page<CampaignResponse> result = campaignUseCase.searchCampaigns(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when no campaigns match")
        void shouldReturnEmptyPage() {
            Page<Campaign> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(campaignRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            CampaignSearchCriteria criteria = CampaignSearchCriteria.builder()
                    .industry("Nonexistent Industry")
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            Page<CampaignResponse> result = campaignUseCase.searchCampaigns(criteria, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should pass criteria with all filters to repository")
        void shouldPassAllFilterCriteria() {
            Page<Campaign> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(campaignRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            CampaignSearchCriteria criteria = CampaignSearchCriteria.builder()
                    .industry("Technology")
                    .offeringType(OfferingType.EQUITY)
                    .status(CampaignStatus.LIVE)
                    .keyword("green")
                    .minTarget(new BigDecimal("10000"))
                    .maxTarget(new BigDecimal("1000000"))
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            campaignUseCase.searchCampaigns(criteria, pageable);

            verify(campaignRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("submitForReview")
    class SubmitForReview {

        @Test
        @DisplayName("should submit complete campaign for review")
        void shouldSubmitCompleteForReview() {
            Campaign campaign = buildCompleteCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignResponse response = campaignUseCase.submitForReview(campaignId, issuerId);

            verify(stateMachine).transition(campaign, CampaignStatus.REVIEW, issuerId);
            verify(campaignRepository).save(campaign);
        }

        @Test
        @DisplayName("should throw when issuer does not own campaign")
        void shouldThrowWhenNotOwner() {
            Campaign campaign = buildCompleteCampaign();
            UUID otherUserId = UUID.randomUUID();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, otherUserId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("do not own");
        }

        @Test
        @DisplayName("should throw when title is still default")
        void shouldThrowWhenTitleIsDefault() {
            Campaign campaign = buildDraftCampaign();
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("title is required");
        }

        @Test
        @DisplayName("should throw when description is missing")
        void shouldThrowWhenDescriptionMissing() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setDescription(null);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        @DisplayName("should throw when description is blank")
        void shouldThrowWhenDescriptionBlank() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setDescription("   ");
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        @DisplayName("should throw when target amount is zero")
        void shouldThrowWhenTargetAmountZero() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setTargetAmount(BigDecimal.ZERO);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Target amount must be set and positive");
        }

        @Test
        @DisplayName("should throw when target amount is negative")
        void shouldThrowWhenTargetAmountNegative() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setTargetAmount(new BigDecimal("-100"));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Target amount must be set and positive");
        }

        @Test
        @DisplayName("should throw when end date is missing")
        void shouldThrowWhenEndDateMissing() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setEndDate(null);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("End date is required");
        }

        @Test
        @DisplayName("should throw when company name is missing")
        void shouldThrowWhenCompanyNameMissing() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setCompanyName(null);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Company name is required");
        }

        @Test
        @DisplayName("should throw when campaign not found")
        void shouldThrowWhenNotFound() {
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignUseCase.submitForReview(campaignId, issuerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("approveCampaign")
    class ApproveCampaign {

        @Test
        @DisplayName("should approve campaign and set start date if null")
        void shouldApproveAndSetStartDate() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setStatus(CampaignStatus.REVIEW);
            campaign.setStartDate(null);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            campaignUseCase.approveCampaign(campaignId, adminId);

            verify(campaignRepository).save(campaignCaptor.capture());
            assertThat(campaignCaptor.getValue().getStartDate()).isNotNull();
            verify(stateMachine).transition(campaign, CampaignStatus.LIVE, adminId);
        }

        @Test
        @DisplayName("should approve campaign and preserve existing start date")
        void shouldPreserveExistingStartDate() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setStatus(CampaignStatus.REVIEW);
            Instant existingStart = Instant.now().minus(1, ChronoUnit.DAYS);
            campaign.setStartDate(existingStart);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            campaignUseCase.approveCampaign(campaignId, adminId);

            verify(campaignRepository).save(campaignCaptor.capture());
            assertThat(campaignCaptor.getValue().getStartDate()).isEqualTo(existingStart);
        }

        @Test
        @DisplayName("should throw when campaign not found")
        void shouldThrowWhenNotFound() {
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignUseCase.approveCampaign(campaignId, adminId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectCampaign")
    class RejectCampaign {

        @Test
        @DisplayName("should reject campaign back to DRAFT")
        void shouldRejectToDraft() {
            Campaign campaign = buildCompleteCampaign();
            campaign.setStatus(CampaignStatus.REVIEW);
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

            campaignUseCase.rejectCampaign(campaignId, "Incomplete information", adminId);

            verify(stateMachine).transition(campaign, CampaignStatus.DRAFT, adminId);
            verify(campaignRepository).save(campaign);
        }

        @Test
        @DisplayName("should throw when campaign not found")
        void shouldThrowWhenNotFound() {
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignUseCase.rejectCampaign(campaignId, "reason", adminId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
