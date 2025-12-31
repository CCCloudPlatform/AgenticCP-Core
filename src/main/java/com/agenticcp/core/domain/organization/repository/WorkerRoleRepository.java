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
 * <p>WorkerRole 엔티티에 대한 데이터 접근을 제공합니다.
 * 설계 C 기준: tenant_id는 제거되었으며, Role이 이미 tenant_id를 가지므로 Role을 통해 테넌트 스코핑이 가능합니다.</p>
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
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.isDeleted = false")
    List<WorkerRole> findByWorkerId(@Param("workerId") Long workerId);
    
    /**
     * Worker ID와 테넌트 ID로 WorkerRole 목록 조회
     * (Role의 tenant_id를 통해 필터링)
     * 
     * @param workerId Worker ID
     * @param tenantId 테넌트 ID
     * @return WorkerRole 목록
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.role.tenant.id = :tenantId AND wr.isDeleted = false")
    List<WorkerRole> findByWorkerIdAndTenantId(@Param("workerId") Long workerId, @Param("tenantId") Long tenantId);
    
    /**
     * 테넌트 ID로 WorkerRole 목록 조회
     * (Role의 tenant_id를 통해 필터링)
     * 
     * @param tenantId 테넌트 ID
     * @return WorkerRole 목록
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.role.tenant.id = :tenantId AND wr.isDeleted = false")
    List<WorkerRole> findByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Worker ID와 Role ID로 WorkerRole 조회
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     * @return WorkerRole (Optional)
     */
    @Query("SELECT wr FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.role.id = :roleId AND wr.isDeleted = false")
    Optional<WorkerRole> findByWorkerIdAndRoleId(@Param("workerId") Long workerId, @Param("roleId") Long roleId);
    
    /**
     * Worker ID와 Role ID로 WorkerRole 존재 여부 확인
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(wr) > 0 FROM WorkerRole wr WHERE wr.worker.id = :workerId AND wr.role.id = :roleId AND wr.isDeleted = false")
    boolean existsByWorkerIdAndRoleId(@Param("workerId") Long workerId, @Param("roleId") Long roleId);
    
    /**
     * Worker ID와 Role ID로 WorkerRole 삭제 (소프트 삭제)
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     */
    @Modifying
    @Query("UPDATE WorkerRole wr SET wr.isDeleted = true WHERE wr.worker.id = :workerId AND wr.role.id = :roleId")
    void deleteByWorkerIdAndRoleId(@Param("workerId") Long workerId, @Param("roleId") Long roleId);
}

