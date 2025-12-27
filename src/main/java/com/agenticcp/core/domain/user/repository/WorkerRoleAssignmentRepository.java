package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.domain.user.entity.WorkerRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Worker Role Assignment Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Repository
public interface WorkerRoleAssignmentRepository extends JpaRepository<WorkerRoleAssignment, Long> {

    /**
     * Tenant ID와 Worker ID로 Role Assignment 목록 조회
     * 만료되지 않은 것만 조회
     * 
     * @param tenantId Tenant ID
     * @param workerId Worker ID
     * @return Role Assignment 목록
     */
    @Query("SELECT wra FROM WorkerRoleAssignment wra " +
           "WHERE wra.tenant.id = :tenantId " +
           "AND wra.worker.id = :workerId " +
           "AND wra.isDeleted = false " +
           "AND (wra.expiresAt IS NULL OR wra.expiresAt > :now)")
    List<WorkerRoleAssignment> findByTenantIdAndWorkerId(
        @Param("tenantId") Long tenantId,
        @Param("workerId") Long workerId,
        @Param("now") LocalDateTime now
    );

    /**
     * Tenant ID와 Worker ID로 Role ID 목록 조회
     * 만료되지 않은 것만 조회
     * 
     * @param tenantId Tenant ID
     * @param workerId Worker ID
     * @return Role ID 목록
     */
    @Query("SELECT wra.role.id FROM WorkerRoleAssignment wra " +
           "WHERE wra.tenant.id = :tenantId " +
           "AND wra.worker.id = :workerId " +
           "AND wra.isDeleted = false " +
           "AND (wra.expiresAt IS NULL OR wra.expiresAt > :now)")
    List<Long> findRoleIdsByTenantIdAndWorkerId(
        @Param("tenantId") Long tenantId,
        @Param("workerId") Long workerId,
        @Param("now") LocalDateTime now
    );
}

