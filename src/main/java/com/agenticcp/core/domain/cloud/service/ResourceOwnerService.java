package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.ResourceOwner;
import com.agenticcp.core.domain.cloud.repository.ResourceOwnerRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 리소스 소유권 관리 서비스
 * 
 * <p>리소스와 사용자 간의 소유권 관계를 관리합니다.
 * DEDICATED 격리 모드에서 리소스 접근 권한을 제어하는데 사용됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceOwnerService {

    private final ResourceOwnerRepository resourceOwnerRepository;

    /**
     * 리소스 소유권 생성
     * 
     * @param resource 리소스
     * @param owner 소유자 (사용자)
     */
    @Transactional
    public void createResourceOwnership(CloudResource resource, User owner) {
        log.info("[ResourceOwnerService] createResourceOwnership - resourceId={}, userId={}",
                LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                LogMaskingUtils.mask(String.valueOf(owner.getId()), 2, 2));

        // 중복 확인
        if (resourceOwnerRepository.existsByResourceAndUserAndIsDeletedFalse(resource, owner)) {
            log.warn("[ResourceOwnerService] createResourceOwnership - already exists resourceId={}, userId={}",
                    LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                    LogMaskingUtils.mask(String.valueOf(owner.getId()), 2, 2));
            return;
        }

        // 소유권 생성
        ResourceOwner resourceOwner = ResourceOwner.builder()
                .resource(resource)
                .user(owner)
                .tenant(resource.getTenant())
                .accessType(ResourceOwner.AccessType.OWNER)
                .build();

        resourceOwnerRepository.save(resourceOwner);
        log.info("[ResourceOwnerService] createResourceOwnership - success resourceId={}, userId={}",
                LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                LogMaskingUtils.mask(String.valueOf(owner.getId()), 2, 2));
    }

    /**
     * 사용자가 소유한 리소스 목록 조회
     * 
     * @param user 사용자
     * @param tenant 테넌트
     * @return 소유한 리소스 목록
     */
    public List<CloudResource> getOwnedResources(User user, Tenant tenant) {
        log.info("[ResourceOwnerService] getOwnedResources - userId={}, tenantKey={}",
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2),
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));

        List<CloudResource> resources = resourceOwnerRepository.findResourcesByOwner(user, tenant);
        
        log.info("[ResourceOwnerService] getOwnedResources - success count={}, userId={}",
                resources.size(),
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));
        return resources;
    }

    /**
     * 리소스 소유권 확인
     * 
     * @param user 사용자
     * @param resource 리소스
     * @return 소유 여부
     */
    public boolean isResourceOwner(User user, CloudResource resource) {
        log.debug("[ResourceOwnerService] isResourceOwner - userId={}, resourceId={}",
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2),
                LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2));

        boolean isOwner = resourceOwnerRepository.existsByResourceAndUserAndIsDeletedFalse(resource, user);
        
        log.debug("[ResourceOwnerService] isResourceOwner - result={}, userId={}, resourceId={}",
                isOwner,
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2),
                LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2));
        return isOwner;
    }

    /**
     * 리소스 소유권 조회
     * 
     * @param resource 리소스
     * @param user 사용자
     * @return 리소스 소유권 정보
     */
    public Optional<ResourceOwner> getResourceOwnership(CloudResource resource, User user) {
        log.debug("[ResourceOwnerService] getResourceOwnership - resourceId={}, userId={}",
                LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));

        return resourceOwnerRepository.findByResourceAndUserAndIsDeletedFalse(resource, user);
    }

    /**
     * 리소스 소유권 삭제 (Soft Delete)
     * 
     * @param resource 리소스
     * @param user 사용자
     */
    @Transactional
    public void deleteResourceOwnership(CloudResource resource, User user) {
        log.info("[ResourceOwnerService] deleteResourceOwnership - resourceId={}, userId={}",
                LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));

        Optional<ResourceOwner> resourceOwner = resourceOwnerRepository
                .findByResourceAndUserAndIsDeletedFalse(resource, user);

        if (resourceOwner.isPresent()) {
            resourceOwner.get().setIsDeleted(true);
            resourceOwnerRepository.save(resourceOwner.get());
            log.info("[ResourceOwnerService] deleteResourceOwnership - success resourceId={}, userId={}",
                    LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                    LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));
        } else {
            log.warn("[ResourceOwnerService] deleteResourceOwnership - not found resourceId={}, userId={}",
                    LogMaskingUtils.mask(String.valueOf(resource.getId()), 2, 2),
                    LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));
        }
    }

    /**
     * 사용자가 소유한 리소스 ID 목록 조회 (배치 최적화)
     * 
     * @param user 사용자
     * @param tenant 테넌트
     * @return 소유한 리소스 ID 목록
     */
    public List<Long> getOwnedResourceIds(User user, Tenant tenant) {
        log.debug("[ResourceOwnerService] getOwnedResourceIds - userId={}, tenantKey={}",
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2),
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));

        List<Long> resourceIds = resourceOwnerRepository.findResourceIdsByOwner(user, tenant);
        
        log.debug("[ResourceOwnerService] getOwnedResourceIds - success count={}, userId={}",
                resourceIds.size(),
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));
        return resourceIds;
    }

    /**
     * 여러 리소스에 대한 사용자 소유권 일괄 확인 (배치 최적화)
     * 
     * @param user 사용자
     * @param resourceIds 리소스 ID 목록
     * @param tenant 테넌트
     * @return 소유한 리소스 ID 목록
     */
    public List<Long> getOwnedResourceIdsBatch(User user, List<Long> resourceIds, Tenant tenant) {
        log.debug("[ResourceOwnerService] getOwnedResourceIdsBatch - userId={}, resourceCount={}, tenantKey={}",
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2),
                resourceIds.size(),
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));

        if (resourceIds.isEmpty()) {
            return List.of();
        }

        List<Long> ownedResourceIds = resourceOwnerRepository.findOwnedResourceIds(user, resourceIds, tenant);
        
        log.debug("[ResourceOwnerService] getOwnedResourceIdsBatch - success ownedCount={}, totalCount={}, userId={}",
                ownedResourceIds.size(),
                resourceIds.size(),
                LogMaskingUtils.mask(String.valueOf(user.getId()), 2, 2));
        return ownedResourceIds;
    }
}

