package com.agenticcp.core.domain.security.repository;

import com.agenticcp.core.domain.security.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByResourceTypeAndResourceIdAndEventTypeOrderByEventTimestampDesc(
            String resourceType,
            String resourceId,
            AuditLog.EventType eventType,
            Pageable pageable
    );
}



