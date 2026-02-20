package com.keza.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditEventRepository auditEventRepository;

    @Async("taskExecutor")
    public void log(String action, String entityType, String entityId, String details) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(getCurrentUser())
                    .ipAddress(getClientIp())
                    .details(details)
                    .build();
            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to save audit event: {} {} {}", action, entityType, entityId, e);
        }
    }

    @Async("taskExecutor")
    public void log(String action, String entityType, String entityId,
                    String oldValue, String newValue, String details) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(getCurrentUser())
                    .ipAddress(getClientIp())
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .details(details)
                    .build();
            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to save audit event: {} {} {}", action, entityType, entityId, e);
        }
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "system";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
