package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudResourceWorkerMap;
import com.agenticcp.core.domain.cloud.entity.CloudResourceWorkerMapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CloudResourceWorkerMap Repository
 * 
 * <p>CloudResourceWorkerMap 엔티티에 대한 데이터 접근을 제공합니다.
 * 설계 C 기준: 리소스 단위로 Worker 접근 권한을 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Repository
public interface CloudResourceWorkerMapRepository extends JpaRepository<CloudResourceWorkerMap, CloudResourceWorkerMapId> {
    
    /**
     * 클라우드 리소스 ID로 CloudResourceWorkerMap 목록 조회
     * 
     * @param resourceId 클라우드 리소스 ID
     * @return CloudResourceWorkerMap 목록
     */
    @Query("SELECT cwm FROM CloudResourceWorkerMap cwm WHERE cwm.cloudResource.id = :resourceId AND cwm.isDeleted = false")
    List<CloudResourceWorkerMap> findByResourceId(@Param("resourceId") Long resourceId);
    
    /**
     * Worker ID로 CloudResourceWorkerMap 목록 조회
     * 
     * @param workerId Worker ID
     * @return CloudResourceWorkerMap 목록
     */
    @Query("SELECT cwm FROM CloudResourceWorkerMap cwm WHERE cwm.worker.id = :workerId AND cwm.isDeleted = false")
    List<CloudResourceWorkerMap> findByWorkerId(@Param("workerId") Long workerId);
    
    /**
     * 클라우드 리소스 ID와 Worker ID로 CloudResourceWorkerMap 조회
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     * @return CloudResourceWorkerMap (Optional)
     */
    @Query("SELECT cwm FROM CloudResourceWorkerMap cwm WHERE cwm.cloudResource.id = :resourceId AND cwm.worker.id = :workerId AND cwm.isDeleted = false")
    Optional<CloudResourceWorkerMap> findByResourceIdAndWorkerId(@Param("resourceId") Long resourceId, @Param("workerId") Long workerId);
    
    /**
     * 클라우드 리소스 ID와 Worker ID로 CloudResourceWorkerMap 존재 여부 확인
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(cwm) > 0 FROM CloudResourceWorkerMap cwm WHERE cwm.cloudResource.id = :resourceId AND cwm.worker.id = :workerId AND cwm.isDeleted = false")
    boolean existsByResourceIdAndWorkerId(@Param("resourceId") Long resourceId, @Param("workerId") Long workerId);
    
    /**
     * 테넌트 ID로 CloudResourceWorkerMap 목록 조회
     * (테넌트의 모든 리소스에 매핑된 Worker 조회)
     * 
     * @param tenantId 테넌트 ID
     * @return CloudResourceWorkerMap 목록
     */
    @Query("SELECT cwm FROM CloudResourceWorkerMap cwm " +
           "WHERE cwm.cloudResource.tenant.id = :tenantId AND cwm.isDeleted = false")
    List<CloudResourceWorkerMap> findByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * 클라우드 리소스 ID와 Worker ID로 CloudResourceWorkerMap 삭제 (소프트 삭제)
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     */
    @Modifying
    @Query("UPDATE CloudResourceWorkerMap cwm SET cwm.isDeleted = true WHERE cwm.cloudResource.id = :resourceId AND cwm.worker.id = :workerId")
    void deleteByResourceIdAndWorkerId(@Param("resourceId") Long resourceId, @Param("workerId") Long workerId);
}

