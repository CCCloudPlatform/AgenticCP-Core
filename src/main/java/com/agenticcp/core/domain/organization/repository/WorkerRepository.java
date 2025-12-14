package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Worker Repository
 * 
 * <p>Worker 엔티티에 대한 데이터 접근을 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    
    /**
     * 사용자 ID로 Worker 목록 조회
     * 
     * @param userId 사용자 ID
     * @return Worker 목록
     */
    List<Worker> findByUserId(Long userId);
    
    /**
     * 테넌트 ID로 Worker 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return Worker 목록
     */
    List<Worker> findByTenantId(Long tenantId);
    
    /**
     * 사용자 ID와 테넌트 ID로 Worker 조회
     * 
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @return Worker (Optional)
     */
    Optional<Worker> findByUserIdAndTenantId(Long userId, Long tenantId);
    
    /**
     * 사용자 ID와 테넌트 ID로 Worker 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);
}

