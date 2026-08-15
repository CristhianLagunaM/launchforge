package com.launchforge.audit.application;

import com.launchforge.audit.api.dto.AuditLogResponse;
import com.launchforge.audit.infrastructure.AuditLogRepository;
import com.launchforge.persistence.model.audit.AuditLog;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.launchforge.shared.exception.ApiBadRequestException;

@Service
public class AuditQueryService {
    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String action,
            String resourceType,
            String actor,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiBadRequestException(
                    "Invalid audit date range",
                    "from must be earlier than or equal to to.",
                    "audit/invalid-date-range"
            );
        }
        Specification<AuditLog> specification = Specification.unrestricted();
        if (hasText(action)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
        }
        if (hasText(resourceType)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("resourceType")), resourceType.trim().toUpperCase()));
        }
        if (hasText(actor)) {
            specification = specification.and(actorSpecification(actor.trim()));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return repository.findAll(specification, pageable).map(AuditLogResponse::from);
    }

    private Specification<AuditLog> actorSpecification(String actor) {
        return (root, query, cb) -> {
            var join = root.join("actorUser", JoinType.INNER);
            try {
                return cb.equal(join.get("id"), UUID.fromString(actor));
            } catch (IllegalArgumentException ignored) {
                return cb.like(cb.lower(join.get("email")), "%" + actor.toLowerCase() + "%");
            }
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
