package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Worker Repository
 * 
 * <p>Worker 엔티티에 대한 데이터 접근을 제공합니다.
 * 설계 C 기준: Worker는 tenant_id를 가지지 않으므로 테넌트별 조회는 CloudResourceWorkerMap을 통해 수행합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    
    /**
     * 사용자 ID로 Worker 목록 조회 (User 기반 Worker)
     * 
     * @param userId 사용자 ID
     * @return Worker 목록
     */
    @Query("SELECT w FROM Worker w WHERE w.user.id = :userId")
    List<Worker> findByUserId(@Param("userId") Long userId);
    
    /**
     * 조직 ID로 Worker 목록 조회 (Organization 기반 Worker)
     * 
     * @param organizationId 조직 ID
     * @return Worker 목록
     */
    @Query("SELECT w FROM Worker w WHERE w.organization.id = :organizationId")
    List<Worker> findByOrganizationId(@Param("organizationId") Long organizationId);
    
    /**
     * 사용자 ID로 Worker 조회 (User 기반 Worker 단건)
     * 
     * @param userId 사용자 ID
     * @return Worker (Optional)
     */
    @Query("SELECT w FROM Worker w WHERE w.user.id = :userId")
    Optional<Worker> findOneByUserId(@Param("userId") Long userId);
    
    /**
     * 조직 ID로 Worker 조회 (Organization 기반 Worker 단건)
     * 
     * @param organizationId 조직 ID
     * @return Worker (Optional)
     */
    @Query("SELECT w FROM Worker w WHERE w.organization.id = :organizationId")
    Optional<Worker> findOneByOrganizationId(@Param("organizationId") Long organizationId);
    
    /**
     * 사용자 ID로 Worker 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(w) > 0 FROM Worker w WHERE w.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
    
    /**
     * 조직 ID로 Worker 존재 여부 확인
     * 
     * @param organizationId 조직 ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(w) > 0 FROM Worker w WHERE w.organization.id = :organizationId")
    boolean existsByOrganizationId(@Param("organizationId") Long organizationId);
}

