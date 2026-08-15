package com.launchforge.audit.api.dto;

import com.launchforge.persistence.model.audit.AuditLog;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        String correlationId,
        String ipAddress,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorUser() == null ? null : log.getActorUser().getId(),
                log.getActorUser() == null ? null : log.getActorUser().getEmail(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getCorrelationId(),
                log.getIpAddress(),
                log.getMetadata() == null ? Map.of() : Map.copyOf(log.getMetadata()),
                log.getCreatedAt()
        );
    }
}
