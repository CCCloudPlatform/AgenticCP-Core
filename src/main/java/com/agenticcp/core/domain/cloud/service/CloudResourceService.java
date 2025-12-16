package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.AuthorizationException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.common.util.SecurityContextUtils;
import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.service.TenantIsolationService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 클라우드 리소스 관리 서비스
 * 
 * <p>리소스 생성/조회/수정/삭제 시 접근 제어를 적용합니다.</p>
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
    private final ResourceOwnerService resourceOwnerService;
    private final ResourceAccessControlService accessControlService;
    private final TenantIsolationService tenantIsolationService;
    private final UserService userService;
    
    /**
     * 리소스 생성
     * 
     * @param resource 리소스 엔티티
     * @return 생성된 리소스
     */
    @Transactional
    public CloudResource createResource(CloudResource resource) {
        log.info("[CloudResourceService] createResource - resourceName={}", resource.getResourceName());

        // 현재 사용자 및 테넌트 정보 가져오기
        User currentUser = getCurrentUser();
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();

        // 리소스에 테넌트 설정
        resource.setTenant(currentTenant);

        // 리소스 저장
        CloudResource saved = cloudResourceRepository.save(resource);

        // 소유권 등록 (중간 테이블에 레코드 추가)
        resourceOwnerService.createResourceOwnership(saved, currentUser);

        log.info("[CloudResourceService] createResource - success resourceId={}, userId={}", 
                saved.getId(), currentUser.getId());
        return saved;
    }

    /**
     * 사용자가 접근 가능한 리소스 목록 조회
     * 
     * @return 접근 가능한 리소스 목록
     */
    public List<CloudResource> getAccessibleResources() {
        log.info("[CloudResourceService] getAccessibleResources");

        User currentUser = getCurrentUser();
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();

        // 격리 수준 조회
        TenantIsolation.IsolationLevel level = tenantIsolationService.getIsolationLevel(currentTenant);

        List<CloudResource> resources;
        if (level == TenantIsolation.IsolationLevel.SHARED) {
            // SHARED: 테넌트의 모든 리소스
            resources = cloudResourceRepository.findByTenantId(currentTenant.getTenantKey());
            log.info("[CloudResourceService] getAccessibleResources - SHARED mode, count={}", resources.size());
        } else {
            // DEDICATED: ResourceOwner 테이블을 통해 소유한 리소스만
            resources = resourceOwnerService.getOwnedResources(currentUser, currentTenant);
            log.info("[CloudResourceService] getAccessibleResources - DEDICATED mode, count={}", resources.size());
        }

        log.info("[CloudResourceService] getAccessibleResources - success count={}", resources.size());
        return resources;
    }

    /**
     * 리소스 조회 (접근 권한 검증)
     * 
     * @param resourceId 리소스 ID
     * @return 리소스
     * @throws ResourceNotFoundException 리소스를 찾을 수 없는 경우
     * @throws com.agenticcp.core.common.exception.AccessDeniedException 접근 권한이 없는 경우
     */
    public CloudResource getResource(Long resourceId) {
        log.info("[CloudResourceService] getResource - resourceId={}", resourceId);

        CloudResource resource = cloudResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));

        // 접근 권한 검증
        User currentUser = getCurrentUser();
        if (!accessControlService.canAccessResource(currentUser, resource)) {
            throw new AuthorizationException(currentUser.getId(), "CloudResource", "조회");
        }

        log.info("[CloudResourceService] getResource - success resourceId={}", resourceId);
        return resource;
    }

    /**
     * 리소스 수정 (접근 권한 검증)
     * 
     * @param resourceId 리소스 ID
     * @param resource 수정할 리소스 정보
     * @return 수정된 리소스
     */
    @Transactional
    public CloudResource updateResource(Long resourceId, CloudResource resource) {
        log.info("[CloudResourceService] updateResource - resourceId={}", resourceId);

        // 기존 리소스 조회 및 접근 권한 검증
        CloudResource existing = getResource(resourceId);

        // 리소스 정보 업데이트
        existing.setResourceName(resource.getResourceName());
        existing.setDisplayName(resource.getDisplayName());
        existing.setStatus(resource.getStatus());
        existing.setLifecycleState(resource.getLifecycleState());
        // 필요한 필드 추가 업데이트

        CloudResource saved = cloudResourceRepository.save(existing);
        log.info("[CloudResourceService] updateResource - success resourceId={}", resourceId);
        return saved;
    }

    /**
     * 리소스 삭제 (접근 권한 검증)
     * 
     * @param resourceId 리소스 ID
     */
    @Transactional
    public void deleteResource(Long resourceId) {
        log.info("[CloudResourceService] deleteResource - resourceId={}", resourceId);

        // 기존 리소스 조회 및 접근 권한 검증
        CloudResource resource = getResource(resourceId);

        // Soft Delete
        resource.setIsDeleted(true);
        cloudResourceRepository.save(resource);

        // ResourceOwner도 Soft Delete
        User currentUser = getCurrentUser();
        resourceOwnerService.deleteResourceOwnership(resource, currentUser);

        log.info("[CloudResourceService] deleteResource - success resourceId={}", resourceId);
    }

    /**
     * 테넌트 키로 클라우드 리소스 목록 조회 (관리자용)
     * 
     * @param tenantKey 테넌트 키
     * @return 클라우드 리소스 목록
     */
    public List<CloudResource> getResourcesByTenant(String tenantKey) {
        log.info("[CloudResourceService] getResourcesByTenant - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
        
        List<CloudResource> resources = cloudResourceRepository.findByTenantKey(tenantKey);
        
        log.info("[CloudResourceService] getResourcesByTenant - success count={} tenantKey={}", 
                resources.size(), LogMaskingUtils.maskTenantKey(tenantKey));
        
        return resources;
    }

    /**
     * 리소스 ID로 조회 (관리자용, 접근 권한 검증 없음)
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

    /**
     * 현재 사용자 조회
     * 
     * @return 현재 사용자
     */
    private User getCurrentUser() {
        String username = SecurityContextUtils.getCurrentUsernameOrThrow();
        return userService.getUserByUsernameOrThrow(username);
    }
}

