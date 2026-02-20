package com.keza.infrastructure.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogger")
class AuditLoggerTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger(auditEventRepository);
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("log(action, entityType, entityId, details)")
    class SimpleLog {

        @Test
        @DisplayName("should save audit event with correct fields")
        void shouldSaveAuditEvent() {
            auditLogger.log("CREATE", "Campaign", "123", "Created new campaign");

            verify(auditEventRepository).save(eventCaptor.capture());
            AuditEvent saved = eventCaptor.getValue();

            assertThat(saved.getAction()).isEqualTo("CREATE");
            assertThat(saved.getEntityType()).isEqualTo("Campaign");
            assertThat(saved.getEntityId()).isEqualTo("123");
            assertThat(saved.getDetails()).isEqualTo("Created new campaign");
        }

        @Test
        @DisplayName("should set performedBy to 'system' when no authentication")
        void shouldDefaultToSystem() {
            auditLogger.log("CREATE", "Campaign", "123", "details");

            verify(auditEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getPerformedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("should set performedBy to authenticated user name")
        void shouldUseAuthenticatedUser() {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken("jane@keza.com", null, java.util.Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            auditLogger.log("UPDATE", "Campaign", "456", "Updated title");

            verify(auditEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getPerformedBy()).isEqualTo("jane@keza.com");
        }

        @Test
        @DisplayName("should set performedBy to 'system' for anonymous user")
        void shouldDefaultToSystemForAnonymous() {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken("anonymousUser", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            auditLogger.log("VIEW", "Campaign", "789", null);

            verify(auditEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getPerformedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("should capture client IP from request")
        void shouldCaptureClientIp() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.100");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            auditLogger.log("CREATE", "User", "1", "Registered");

            verify(auditEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getIpAddress()).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("should use X-Forwarded-For header when present")
        void shouldUseXForwardedForHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            auditLogger.log("CREATE", "User", "2", "Registered");

            verify(auditEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("should set IP to null when no request context")
        void shouldSetNullIpWithoutRequest() {
            auditLogger.log("DELETE", "Campaign", "99", "Deleted");

            verify(auditEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getIpAddress()).isNull();
        }

        @Test
        @DisplayName("should not throw exception when repository save fails")
        void shouldNotThrowOnSaveFailure() {
            doThrow(new RuntimeException("DB connection failed")).when(auditEventRepository).save(any());

            // Should not propagate the exception
            auditLogger.log("CREATE", "Campaign", "123", "details");

            verify(auditEventRepository).save(any());
        }
    }

    @Nested
    @DisplayName("log(action, entityType, entityId, oldValue, newValue, details)")
    class LogWithOldNewValues {

        @Test
        @DisplayName("should save audit event with old and new values")
        void shouldSaveWithOldNewValues() {
            auditLogger.log("UPDATE", "Campaign", "123", "Draft", "Published", "Status change");

            verify(auditEventRepository).save(eventCaptor.capture());
            AuditEvent saved = eventCaptor.getValue();

            assertThat(saved.getAction()).isEqualTo("UPDATE");
            assertThat(saved.getEntityType()).isEqualTo("Campaign");
            assertThat(saved.getEntityId()).isEqualTo("123");
            assertThat(saved.getOldValue()).isEqualTo("Draft");
            assertThat(saved.getNewValue()).isEqualTo("Published");
            assertThat(saved.getDetails()).isEqualTo("Status change");
        }

        @Test
        @DisplayName("should handle null old and new values")
        void shouldHandleNullValues() {
            auditLogger.log("UPDATE", "Campaign", "123", null, null, "Minor update");

            verify(auditEventRepository).save(eventCaptor.capture());
            AuditEvent saved = eventCaptor.getValue();

            assertThat(saved.getOldValue()).isNull();
            assertThat(saved.getNewValue()).isNull();
        }

        @Test
        @DisplayName("should not throw exception when repository save fails")
        void shouldNotThrowOnSaveFailure() {
            doThrow(new RuntimeException("DB error")).when(auditEventRepository).save(any());

            auditLogger.log("UPDATE", "Campaign", "1", "old", "new", "details");

            verify(auditEventRepository).save(any());
        }
    }
}
