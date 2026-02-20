package com.keza.infrastructure.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogger auditLogger;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        String entityId = extractEntityId(joinPoint.getArgs());
        String details = String.format("Method: %s.%s",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName());

        auditLogger.log(audited.action(), audited.entityType(), entityId, details);

        return result;
    }

    private String extractEntityId(Object[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }
}
