package com.agenticcp.core.domain.cloud.service.helper;

import com.agenticcp.core.common.context.TenantContextHolder;
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
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudResourceManagementHelper {

    private final CloudResourceRepository cloudResourceRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final TenantRepository tenantRepository;

    // ==================== VM Instance ====================

    /**
     * VM 인스턴스를 CloudResource로 등록합니다.
     *
     * @param providerType 프로바이더 타입
     * @param serviceKey   서비스 키 (EC2, VirtualMachines 등)
     * @param instanceId   인스턴스 ID
     * @param resourceName 리소스 이름
     * @param instanceSize 인스턴스 크기
     * @param tags         태그 맵
     */
    @Transactional
    public void registerVmInstance(
            ProviderType providerType,
            String serviceKey,
            String instanceId,
            String resourceName,
            String instanceSize,
            Map<String, String> tags
    ) {
        try {
            CloudProvider provider = findProvider(providerType);
            CloudService service = findServiceOrNull(providerType, serviceKey);
            Tenant tenant = findCurrentTenant();

            CloudResource cloudResource = CloudResource.createVmInstance(
                    instanceId,
                    resourceName,
                    provider,
                    service,
                    tenant,
                    instanceSize,
                    tags
            );

            cloudResourceRepository.save(cloudResource);
            log.debug("[CloudResourceManagementHelper] VM 인스턴스 등록 완료: instanceId={}", instanceId);

        } catch (Exception e) {
            log.warn("[CloudResourceManagementHelper] VM 인스턴스 등록 실패: instanceId={}, error={}",
                    instanceId, e.getMessage());
        }
    }

    // ==================== Storage Bucket ====================

    /**
     * Object Storage 버킷을 CloudResource로 등록합니다.
     *
     * @param providerType  프로바이더 타입
     * @param serviceKey    서비스 키 (S3, BlobStorage 등)
     * @param containerName 컨테이너/버킷 이름
     * @param tags          태그 맵
     */
    @Transactional
    public void registerStorageBucket(
            ProviderType providerType,
            String serviceKey,
            String containerName,
            Map<String, String> tags
    ) {
        try {
            CloudProvider provider = findProvider(providerType);
            CloudService service = findServiceOrNull(providerType, serviceKey);
            Tenant tenant = findCurrentTenant();

            CloudResource cloudResource = CloudResource.createStorageBucket(
                    containerName,
                    provider,
                    service,
                    tenant,
                    tags
            );

            cloudResourceRepository.save(cloudResource);
            log.debug("[CloudResourceManagementHelper] 스토리지 버킷 등록 완료: containerName={}", containerName);

        } catch (Exception e) {
            log.warn("[CloudResourceManagementHelper] 스토리지 버킷 등록 실패: containerName={}, error={}",
                    containerName, e.getMessage());
        }
    }

    // ==================== VPC ====================

    /**
     * VPC를 CloudResource로 등록합니다.
     *
     * @param providerType 프로바이더 타입
     * @param serviceKey   서비스 키 (EC2, VirtualNetwork 등)
     * @param vpcId        VPC ID
     * @param resourceName 리소스 이름 (VPC 이름 또는 vpcId)
     * @param cidrBlock    CIDR 블록
     * @param tags         태그 맵
     */
    @Transactional
    public void registerVpc(
            ProviderType providerType,
            String serviceKey,
            String vpcId,
            String resourceName,
            String cidrBlock,
            Map<String, String> tags
    ) {
        try {
            CloudProvider provider = findProvider(providerType);
            CloudService service = findServiceOrThrow(providerType, serviceKey);
            Tenant tenant = findCurrentTenant();

            CloudResource cloudResource = CloudResource.createVpc(
                    vpcId,
                    resourceName,
                    provider,
                    service,
                    tenant,
                    cidrBlock,
                    tags
            );

            cloudResourceRepository.save(cloudResource);
            log.debug("[CloudResourceManagementHelper] VPC 등록 완료: vpcId={}", vpcId);

        } catch (Exception e) {
            log.warn("[CloudResourceManagementHelper] VPC 등록 실패: vpcId={}, error={}",
                    vpcId, e.getMessage());
        }
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

    private CloudService findServiceOrNull(ProviderType providerType, String serviceKey) {
        return cloudServiceRepository
                .findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElse(null);
    }

    private CloudService findServiceOrThrow(ProviderType providerType, String serviceKey) {
        return cloudServiceRepository
                .findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElseThrow(() -> new IllegalStateException(
                        "CloudService not found for provider: " + providerType + ", serviceKey: " + serviceKey));
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

