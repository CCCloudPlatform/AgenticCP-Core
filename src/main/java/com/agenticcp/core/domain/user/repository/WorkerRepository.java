package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.domain.user.entity.Worker;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Worker Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    /**
     * User ID로 Worker 목록 조회
     * 
     * @param userId User ID
     * @return Worker 목록
     */
    List<Worker> findByUserIdAndIsDeletedFalse(Long userId);

    /**
     * User ID와 Tenant ID로 Worker 조회
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Worker (Optional)
     */
    Optional<Worker> findByUserIdAndTenantIdAndIsDeletedFalse(Long userId, Long tenantId);

    /**
     * Tenant ID로 Worker 목록 조회
     * 
     * @param tenantId Tenant ID
     * @return Worker 목록
     */
    List<Worker> findByTenantIdAndIsDeletedFalse(Long tenantId);

    /**
     * Worker Key로 Worker 조회
     * 
     * @param workerKey Worker Key
     * @return Worker (Optional)
     */
    Optional<Worker> findByWorkerKeyAndIsDeletedFalse(String workerKey);

    /**
     * User가 속한 모든 Tenant ID 목록 조회
     * 
     * @param userId User ID
     * @return Tenant ID 목록
     */
    @Query("SELECT DISTINCT w.tenant.id FROM Worker w WHERE w.user.id = :userId AND w.isDeleted = false")
    List<Long> findTenantIdsByUserId(@Param("userId") Long userId);

    /**
     * User가 특정 Tenant에 속하는지 확인
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndTenantIdAndIsDeletedFalse(Long userId, Long tenantId);
}

