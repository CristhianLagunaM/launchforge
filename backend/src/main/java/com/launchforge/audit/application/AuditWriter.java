package com.launchforge.audit.application;

import com.launchforge.audit.infrastructure.AuditLogRepository;
import com.launchforge.persistence.model.audit.AuditLog;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.shared.web.RequestAuditContext;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditWriter {
    private final AuditLogRepository repository;
    private final EntityManager entityManager;

    public AuditWriter(AuditLogRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(AuditAction action, String resource, String resourceId, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        currentActorId().ifPresent(id -> log.setActorUser(entityManager.getReference(User.class, id)));
        log.setAction(action.name());
        log.setResourceType(resource);
        log.setResourceId(resourceId);
        RequestAuditContext.Context context = RequestAuditContext.current();
        if (context != null) {
            log.setCorrelationId(context.correlationId());
            log.setIpAddress(context.ipAddress());
        }
        log.setMetadata(metadata.isEmpty() ? null : metadata);
        repository.save(log);
    }

    private java.util.Optional<UUID> currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(UUID.fromString(jwt.getSubject()));
    }
}
