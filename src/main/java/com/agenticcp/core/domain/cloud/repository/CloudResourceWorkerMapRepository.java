package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudResourceWorkerMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cloud Resource Worker Map Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Repository
public interface CloudResourceWorkerMapRepository extends JpaRepository<CloudResourceWorkerMap, Long> {

    /**
     * Cloud Resource ID로 Worker Map 목록 조회
     * 만료되지 않은 것만 조회
     * 
     * @param cloudResourceId Cloud Resource ID
     * @return Worker Map 목록
     */
    @Query("SELECT crwm FROM CloudResourceWorkerMap crwm " +
           "WHERE crwm.cloudResource.id = :cloudResourceId " +
           "AND crwm.isDeleted = false " +
           "AND (crwm.expiresAt IS NULL OR crwm.expiresAt > :now)")
    List<CloudResourceWorkerMap> findByCloudResourceId(
        @Param("cloudResourceId") Long cloudResourceId,
        @Param("now") LocalDateTime now
    );

    /**
     * Worker ID로 Worker Map 목록 조회
     * 만료되지 않은 것만 조회
     * 
     * @param workerId Worker ID
     * @return Worker Map 목록
     */
    @Query("SELECT crwm FROM CloudResourceWorkerMap crwm " +
           "WHERE crwm.worker.id = :workerId " +
           "AND crwm.isDeleted = false " +
           "AND (crwm.expiresAt IS NULL OR crwm.expiresAt > :now)")
    List<CloudResourceWorkerMap> findByWorkerId(
        @Param("workerId") Long workerId,
        @Param("now") LocalDateTime now
    );

    /**
     * Cloud Resource ID와 Worker ID로 Worker Map 조회
     * 만료되지 않은 것만 조회
     * 
     * @param cloudResourceId Cloud Resource ID
     * @param workerId Worker ID
     * @return Worker Map (Optional)
     */
    @Query("SELECT crwm FROM CloudResourceWorkerMap crwm " +
           "WHERE crwm.cloudResource.id = :cloudResourceId " +
           "AND crwm.worker.id = :workerId " +
           "AND crwm.isDeleted = false " +
           "AND (crwm.expiresAt IS NULL OR crwm.expiresAt > :now)")
    Optional<CloudResourceWorkerMap> findByCloudResourceIdAndWorkerId(
        @Param("cloudResourceId") Long cloudResourceId,
        @Param("workerId") Long workerId,
        @Param("now") LocalDateTime now
    );

    /**
     * Worker가 접근 가능한 Cloud Resource ID 목록 조회
     * 만료되지 않은 것만 조회
     * 
     * @param workerId Worker ID
     * @return Cloud Resource ID 목록
     */
    @Query("SELECT crwm.cloudResource.id FROM CloudResourceWorkerMap crwm " +
           "WHERE crwm.worker.id = :workerId " +
           "AND crwm.isDeleted = false " +
           "AND (crwm.expiresAt IS NULL OR crwm.expiresAt > :now)")
    List<Long> findCloudResourceIdsByWorkerId(
        @Param("workerId") Long workerId,
        @Param("now") LocalDateTime now
    );

    /**
     * Worker가 특정 Cloud Resource에 접근 가능한지 확인
     * 만료되지 않은 것만 확인
     * 
     * @param cloudResourceId Cloud Resource ID
     * @param workerId Worker ID
     * @return 접근 가능 여부
     */
    @Query("SELECT CASE WHEN COUNT(crwm) > 0 THEN true ELSE false END " +
           "FROM CloudResourceWorkerMap crwm " +
           "WHERE crwm.cloudResource.id = :cloudResourceId " +
           "AND crwm.worker.id = :workerId " +
           "AND crwm.isDeleted = false " +
           "AND (crwm.expiresAt IS NULL OR crwm.expiresAt > :now)")
    boolean existsByCloudResourceIdAndWorkerId(
        @Param("cloudResourceId") Long cloudResourceId,
        @Param("workerId") Long workerId,
        @Param("now") LocalDateTime now
    );
}

