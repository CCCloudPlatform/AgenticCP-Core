package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 클라우드 리소스 관리 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudResourceService {
    
    private final CloudResourceRepository cloudResourceRepository;
    
    /**
     * 테넌트 키로 클라우드 리소스 목록 조회
     * 
     * @param tenantKey 테넌트 키 (tenantKey)
     * @return 클라우드 리소스 목록
     */
    public List<CloudResource> getResourcesByTenant(String tenantKey) {
        log.info("[CloudResourceService] getResourcesByTenant - tenantKey={}", 
                LogMaskingUtils.mask(tenantKey, 2, 2));
        
        List<CloudResource> resources = cloudResourceRepository.findByTenantKey(tenantKey);
        
        log.info("[CloudResourceService] getResourcesByTenant - success count={} tenantId={}", 
                resources.size(), LogMaskingUtils.mask(tenantKey, 2, 2));
        
        return resources;
    }
    
    /**
     * 리소스 ID로 조회
     * 
     * @param resourceId 리소스 ID
     * @return 클라우드 리소스
     */
    public CloudResource getResourceById(String resourceId) {
        log.info("[CloudResourceService] getResourceById - resourceId={}", 
                LogMaskingUtils.mask(resourceId, 2, 2));
        
        CloudResource resource = cloudResourceRepository.findByResourceId(resourceId);
        
        log.info("[CloudResourceService] getResourceById - found={} resourceId={}", 
                resource != null, LogMaskingUtils.mask(resourceId, 2, 2));
        
        return resource;
    }
}

