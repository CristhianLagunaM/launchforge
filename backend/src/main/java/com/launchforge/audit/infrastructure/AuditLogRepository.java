package com.launchforge.audit.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.launchforge.persistence.model.audit.AuditLog;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>,
                JpaSpecificationExecutor<AuditLog> {

    @Override
    @SuppressWarnings("null")
    @EntityGraph(attributePaths = "actorUser")
    Page<AuditLog> findAll(
            Specification<AuditLog> specification,
            Pageable pageable
    );
}
