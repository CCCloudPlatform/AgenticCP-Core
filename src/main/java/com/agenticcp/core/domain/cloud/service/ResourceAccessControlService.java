package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.service.TenantIsolationService;
import com.agenticcp.core.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 리소스 접근 제어 서비스
 * 
 * <p>테넌트 격리 수준에 따라 리소스 접근 권한을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAccessControlService {

    private final TenantIsolationService tenantIsolationService;
    private final ResourceOwnerService resourceOwnerService;

    /**
     * 리소스 접근 권한 검증
     * 
     * @param user 사용자
     * @param resource 리소스
     * @return 접근 가능 여부
     */
    public boolean canAccessResource(User user, CloudResource resource) {
        log.debug("[ResourceAccessControlService] canAccessResource - userId={}, resourceId={}",
                user.getId(), resource.getId());

        // 1. 테넌트 일치 확인
        if (!isSameTenant(user.getTenant(), resource.getTenant())) {
            log.warn("[ResourceAccessControlService] canAccessResource - denied: different tenant - userId={}, resourceId={}",
                    user.getId(), resource.getId());
            return false;
        }

        // 2. 격리 수준 조회
        TenantIsolation.IsolationLevel level = tenantIsolationService.getIsolationLevel(resource.getTenant());

        // 3. 격리 수준이 없으면 기본적으로 거부 (안전한 기본값)
        if (level == null) {
            log.warn("[ResourceAccessControlService] canAccessResource - denied: isolation level not set - tenantId={}",
                    resource.getTenant().getId());
            return false;
        }

        // 4. SHARED 모드: 테넌트 내 모든 사용자 접근 가능
        if (level == TenantIsolation.IsolationLevel.SHARED) {
            log.debug("[ResourceAccessControlService] canAccessResource - granted: SHARED mode - userId={}, resourceId={}",
                    user.getId(), resource.getId());
            return true;
        }

        // 5. DEDICATED 모드: ResourceOwner 테이블에서 소유권 확인
        if (level == TenantIsolation.IsolationLevel.DEDICATED) {
            boolean isOwner = resourceOwnerService.isResourceOwner(user, resource);
            if (isOwner) {
                log.debug("[ResourceAccessControlService] canAccessResource - granted: DEDICATED mode (owner) - userId={}, resourceId={}",
                        user.getId(), resource.getId());
            } else {
                log.warn("[ResourceAccessControlService] canAccessResource - denied: DEDICATED mode (not owner) - userId={}, resourceId={}",
                        user.getId(), resource.getId());
            }
            return isOwner;
        }

        // 6. 알 수 없는 격리 수준
        log.error("[ResourceAccessControlService] canAccessResource - denied: unknown isolation level - level={}, tenantId={}",
                level, resource.getTenant().getId());
        return false;
    }

    /**
     * 접근 가능한 리소스만 필터링
     * 
     * @param user 사용자
     * @param resources 리소스 목록
     * @return 접근 가능한 리소스 목록
     */
    public List<CloudResource> filterAccessibleResources(User user, List<CloudResource> resources) {
        log.debug("[ResourceAccessControlService] filterAccessibleResources - userId={}, resourceCount={}",
                user.getId(), resources.size());

        if (resources.isEmpty()) {
            return List.of();
        }

        // 1. 테넌트 일치 확인
        Tenant userTenant = user.getTenant();
        List<CloudResource> sameTenantResources = resources.stream()
                .filter(resource -> isSameTenant(userTenant, resource.getTenant()))
                .collect(Collectors.toList());

        if (sameTenantResources.isEmpty()) {
            log.debug("[ResourceAccessControlService] filterAccessibleResources - no same tenant resources");
            return List.of();
        }

        // 2. 격리 수준 조회 (한 번만)
        TenantIsolation.IsolationLevel level = tenantIsolationService.getIsolationLevel(userTenant);

        // 3. SHARED 모드: 테넌트 일치한 모든 리소스 반환
        if (level == TenantIsolation.IsolationLevel.SHARED) {
            log.debug("[ResourceAccessControlService] filterAccessibleResources - SHARED mode, returning all same tenant resources");
            return sameTenantResources;
        }

        // 4. DEDICATED 모드: 배치로 소유권 확인 (N+1 문제 해결)
        if (level == TenantIsolation.IsolationLevel.DEDICATED) {
            List<Long> resourceIds = sameTenantResources.stream()
                    .map(CloudResource::getId)
                    .collect(Collectors.toList());

            // 배치로 소유한 리소스 ID 조회 (한 번의 쿼리)
            List<Long> ownedResourceIds = resourceOwnerService.getOwnedResourceIdsBatch(
                    user, resourceIds, userTenant);

            // 소유한 리소스만 필터링
            Set<Long> ownedResourceIdSet = new HashSet<>(ownedResourceIds);
            List<CloudResource> accessibleResources = sameTenantResources.stream()
                    .filter(resource -> ownedResourceIdSet.contains(resource.getId()))
                    .collect(Collectors.toList());

            log.debug("[ResourceAccessControlService] filterAccessibleResources - DEDICATED mode, accessibleCount={}, totalCount={}",
                    accessibleResources.size(), resources.size());
            return accessibleResources;
        }

        // 5. 알 수 없는 격리 수준: 접근 거부
        log.warn("[ResourceAccessControlService] filterAccessibleResources - unknown isolation level: {}", level);
        return List.of();
    }

    /**
     * 테넌트 일치 확인
     * 
     * @param userTenant 사용자의 테넌트
     * @param resourceTenant 리소스의 테넌트
     * @return 테넌트 일치 여부
     */
    private boolean isSameTenant(Tenant userTenant, Tenant resourceTenant) {
        if (userTenant == null || resourceTenant == null) {
            return false;
        }
        return userTenant.getId().equals(resourceTenant.getId());
    }
}

