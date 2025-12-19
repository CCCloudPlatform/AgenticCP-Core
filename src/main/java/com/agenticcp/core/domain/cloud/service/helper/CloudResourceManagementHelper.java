package com.agenticcp.core.domain.cloud.service.helper;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * CloudResource 관리를 위한 Helper 컴포넌트
 * 
 * 서비스 레이어의 Repository 의존성을 줄이고, 연관 엔티티 조회 및 
 * CloudResource 등록/삭제/상태변경 로직을 중앙화합니다.
 * 
 * 주의: 등록 메서드(registerXxx)는 실패 시 예외를 던집니다.
 * 호출하는 서비스에서 적절한 예외 처리(보상 트랜잭션 등)를 해야 합니다.
 *
 * @author AgenticCP Team
 * @version 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudResourceManagementHelper {

    private final CloudResourceRepository cloudResourceRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final TenantRepository tenantRepository;

    // ==================== 통합 리소스 등록 ====================

    /**
     * CloudResource를 통합적으로 등록합니다.
     * 
     * 모든 리소스 타입(VM, Storage, VPC, RDS 등)을 하나의 메서드로 등록할 수 있습니다.
     * 도메인별 상세 속성은 ResourceRegistrationRequest의 attributes에 담아 전달합니다.
     * 
     * <p><b>주의:</b> 실패 시 예외를 던집니다. 
     * 호출하는 서비스에서 보상 트랜잭션을 처리해야 합니다.</p>
     *
     * @param providerType 프로바이더 타입
     * @param serviceKey   서비스 키 (EC2, S3, VirtualMachines 등)
     * @param request      리소스 등록 요청 DTO
     * @return 저장된 CloudResource 엔티티
     */
    @Transactional
    public CloudResource registerResource(
            ProviderType providerType,
            String serviceKey,
            ResourceRegistrationRequest request
    ) {
        CloudProvider provider = findProvider(providerType);
        CloudService service = findServiceOrCreate(providerType, serviceKey, provider);
        Tenant tenant = findCurrentTenant();

        CloudResource cloudResource = CloudResource.create(request, provider, service, tenant);

        CloudResource savedResource = cloudResourceRepository.save(cloudResource);
        log.debug("[CloudResourceManagementHelper] 리소스 등록 완료: resourceType={}, resourceId={}", 
                request.getResourceType(), savedResource.getResourceId());
        return savedResource;
    }

    // ==================== 공통 작업 ====================

    /**
     * 리소스의 생명주기 상태를 업데이트합니다.
     *
     * @param resourceId     리소스 ID
     * @param lifecycleState 새로운 생명주기 상태
     */
    public void updateLifecycleState(String resourceId, LifecycleState lifecycleState) {
        try {
            int updatedCount = cloudResourceRepository.updateLifecycleState(
                    resourceId, lifecycleState, LocalDateTime.now());

            if (updatedCount > 0) {
                log.debug("[CloudResourceManagementHelper] 생명주기 상태 업데이트 완료: resourceId={}, state={}",
                        resourceId, lifecycleState);
            } else {
                log.debug("[CloudResourceManagementHelper] DB에 리소스가 없어 상태 업데이트 스킵: resourceId={}", resourceId);
            }
        } catch (Exception e) {
            log.warn("[CloudResourceManagementHelper] 생명주기 상태 업데이트 실패: resourceId={}, error={}",
                    resourceId, e.getMessage());
        }
    }

    /**
     * 리소스를 소프트 삭제합니다.
     *
     * @param resourceId 리소스 ID
     */
    public void softDeleteResource(String resourceId) {
        try {
            int deletedCount = cloudResourceRepository.softDeleteByResourceId(resourceId);

            if (deletedCount > 0) {
                log.debug("[CloudResourceManagementHelper] 리소스 소프트 삭제 완료: resourceId={}", resourceId);
            } else {
                log.debug("[CloudResourceManagementHelper] DB에 리소스가 없어 삭제 스킵: resourceId={}", resourceId);
            }
        } catch (Exception e) {
            log.warn("[CloudResourceManagementHelper] 리소스 소프트 삭제 실패: resourceId={}, error={}",
                    resourceId, e.getMessage());
        }
    }

    // ==================== Private Helper Methods ====================

    private CloudProvider findProvider(ProviderType providerType) {
        return cloudProviderRepository.findFirstByProviderType(providerType)
                .orElseThrow(() -> new IllegalStateException(
                        "CloudProvider not found for type: " + providerType));
    }

    private CloudService findServiceOrCreate(ProviderType providerType, String serviceKey, CloudProvider provider) {
        return cloudServiceRepository
                .findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElseGet(() -> {
                    log.info("[CloudResourceManagementHelper] CloudService not found, creating: providerType={}, serviceKey={}", 
                            providerType, serviceKey);
                    CloudService newService = createDefaultService(providerType, serviceKey, provider);
                    return cloudServiceRepository.save(newService);
                });
    }

    private CloudService createDefaultService(ProviderType providerType, String serviceKey, CloudProvider provider) {
        return CloudService.builder()
                .serviceKey(serviceKey)
                .serviceName(serviceKey)
                .displayName(null)
                .provider(provider)
                .status(com.agenticcp.core.common.enums.Status.ACTIVE)
                .serviceType(null)
                .serviceCategory(null)
                .isRegionSpecific(true)
                .build();
    }

    private Tenant findCurrentTenant() {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        return tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant not found for key: " + tenantKey));
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 태그에서 리소스 이름 추출 (Name 태그 또는 기본값 사용)
     *
     * @param tags       태그 맵
     * @param defaultName 기본 이름 (Name 태그가 없을 경우)
     * @return 리소스 이름
     */
    public String extractResourceName(Map<String, String> tags, String defaultName) {
        if (tags != null && tags.containsKey("Name")) {
            return tags.get("Name");
        }
        return defaultName;
    }
}

