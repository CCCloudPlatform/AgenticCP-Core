package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.TenantWorkerMap;
import com.agenticcp.core.domain.organization.entity.TenantWorkerMapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TenantWorkerMap Repository
 * 
 * <p>TenantWorkerMap 엔티티에 대한 데이터 접근을 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Repository
public interface TenantWorkerMapRepository extends JpaRepository<TenantWorkerMap, TenantWorkerMapId> {
    
    /**
     * 테넌트 ID로 TenantWorkerMap 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return TenantWorkerMap 목록
     */
    @Query("SELECT twm FROM TenantWorkerMap twm WHERE twm.tenant.id = :tenantId")
    List<TenantWorkerMap> findByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Worker ID로 TenantWorkerMap 목록 조회
     * 
     * @param workerId Worker ID
     * @return TenantWorkerMap 목록
     */
    @Query("SELECT twm FROM TenantWorkerMap twm WHERE twm.worker.id = :workerId")
    List<TenantWorkerMap> findByWorkerId(@Param("workerId") Long workerId);
    
    /**
     * 테넌트 ID와 Worker ID로 TenantWorkerMap 조회
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     * @return TenantWorkerMap (Optional)
     */
    @Query("SELECT twm FROM TenantWorkerMap twm WHERE twm.tenant.id = :tenantId AND twm.worker.id = :workerId")
    Optional<TenantWorkerMap> findByTenantIdAndWorkerId(@Param("tenantId") Long tenantId, @Param("workerId") Long workerId);
    
    /**
     * 테넌트 ID와 Worker ID로 TenantWorkerMap 존재 여부 확인
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(twm) > 0 FROM TenantWorkerMap twm WHERE twm.tenant.id = :tenantId AND twm.worker.id = :workerId")
    boolean existsByTenantIdAndWorkerId(@Param("tenantId") Long tenantId, @Param("workerId") Long workerId);
    
    /**
     * 테넌트 ID와 Worker ID로 TenantWorkerMap 삭제
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     */
    @Modifying
    @Query("DELETE FROM TenantWorkerMap twm WHERE twm.tenant.id = :tenantId AND twm.worker.id = :workerId")
    void deleteByTenantIdAndWorkerId(@Param("tenantId") Long tenantId, @Param("workerId") Long workerId);
}

