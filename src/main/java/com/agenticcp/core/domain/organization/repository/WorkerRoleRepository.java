package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.WorkerRole;
import com.agenticcp.core.domain.organization.entity.WorkerRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WorkerRole Repository
 * 
 * <p>WorkerRole 엔티티에 대한 데이터 접근을 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Repository
public interface WorkerRoleRepository extends JpaRepository<WorkerRole, WorkerRoleId> {
    
    /**
     * Worker ID로 WorkerRole 목록 조회
     * 
     * @param workerId Worker ID
     * @return WorkerRole 목록
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.worker.id = :workerId")
    List<WorkerRole> findByWorkerId(@Param("workerId") Long workerId);
    
    /**
     * Worker ID와 테넌트 ID로 WorkerRole 목록 조회
     * 
     * @param workerId Worker ID
     * @param tenantId 테넌트 ID
     * @return WorkerRole 목록
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.tenant.id = :tenantId")
    List<WorkerRole> findByWorkerIdAndTenantId(@Param("workerId") Long workerId, @Param("tenantId") Long tenantId);
    
    /**
     * 테넌트 ID로 WorkerRole 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return WorkerRole 목록
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.tenant.id = :tenantId")
    List<WorkerRole> findByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Worker ID, 테넌트 ID, Role ID로 WorkerRole 조회
     * 
     * @param workerId Worker ID
     * @param tenantId 테넌트 ID
     * @param roleId Role ID
     * @return WorkerRole (Optional)
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.tenant.id = :tenantId AND wr.role.id = :roleId")
    Optional<WorkerRole> findByWorkerIdAndTenantIdAndRoleId(@Param("workerId") Long workerId, @Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
    
    /**
     * Worker ID, 테넌트 ID, Role ID로 WorkerRole 존재 여부 확인
     * 
     * @param workerId Worker ID
     * @param tenantId 테넌트 ID
     * @param roleId Role ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(wr) > 0 FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.tenant.id = :tenantId AND wr.role.id = :roleId")
    boolean existsByWorkerIdAndTenantIdAndRoleId(@Param("workerId") Long workerId, @Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
    
    /**
     * Worker ID, 테넌트 ID, Role ID로 WorkerRole 삭제
     * 
     * @param workerId Worker ID
     * @param tenantId 테넌트 ID
     * @param roleId Role ID
     */
    @Modifying
    @Query("DELETE FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.tenant.id = :tenantId AND wr.role.id = :roleId")
    void deleteByWorkerIdAndTenantIdAndRoleId(@Param("workerId") Long workerId, @Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}

