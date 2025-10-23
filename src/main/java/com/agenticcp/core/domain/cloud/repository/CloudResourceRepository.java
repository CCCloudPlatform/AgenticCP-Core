package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 클라우드 리소스 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-06
 */
@Repository
public interface CloudResourceRepository extends TenantAwareRepository<CloudResource, Long> {
    
    /**
     * 프로바이더별 클라우드 리소스 목록 조회
     * 
     * @param providerId 프로바이더 ID
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.provider.id = :providerId " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByProviderId(@Param("providerId") Long providerId);
    
    /**
     * 리소스 ID로 조회
     * 
     * @param resourceId 리소스 ID
     * @return 클라우드 리소스
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    CloudResource findByResourceId(@Param("resourceId") String resourceId);
}

