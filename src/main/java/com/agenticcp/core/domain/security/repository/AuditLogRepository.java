package com.agenticcp.core.domain.security.repository;

import com.agenticcp.core.domain.security.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @QueryHints(value = {
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true"),
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "100")
    })
    Page<AuditLog> findByResourceTypeAndResourceIdAndEventTypeOrderByEventTimestampDesc(
            String resourceType,
            String resourceId,
            AuditLog.EventType eventType,
            Pageable pageable
    );
}



