package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 감사 로그 리포지토리
 * 
 * 감사 로그 데이터의 영속성을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 대상 리소스 ID로 감사 로그 조회 (최근순)
     */
    List<AuditLog> findByTargetResourceIdOrderByTimestampDesc(String targetResourceId);


    /**
     * 특정 대상의 변경 이력 조회 (변경 추적이 있는 것만)
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.targetResourceId = :targetResourceId AND " +
           "(a.oldValue IS NOT NULL OR a.newValue IS NOT NULL) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findChangeHistoryByTargetResourceId(
        @Param("targetResourceId") String targetResourceId
    );

    /**
     * 리소스 타입과 대상 ID로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByResourceTypeAndTargetResourceId(
        AuditResourceType resourceType,
        String targetResourceId,
        Pageable pageable
    );
}

