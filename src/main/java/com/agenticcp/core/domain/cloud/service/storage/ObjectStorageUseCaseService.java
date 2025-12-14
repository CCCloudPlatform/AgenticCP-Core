package com.agenticcp.core.domain.cloud.service.storage;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.CreateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.dto.ObjectStorageContainerQueryRequest;
import com.agenticcp.core.domain.cloud.dto.UpdateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.CreateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.model.storage.UpdateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Object Storage Container 유스케이스 서비스
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 Object Storage Container 관련 비즈니스 로직을 처리합니다.
 * 포트 인터페이스를 통해서만 외부 시스템과 통신하며, 트랜잭션, 감사, 추적을 담당합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageUseCaseService {

    private final ObjectStoragePortRouter router;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    
    // DB 저장을 위한 Repository
    private final CloudResourceRepository cloudResourceRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final TenantRepository tenantRepository;

    private static final String RESOURCE_TYPE = "BUCKET";

    /**
     * Object Storage Container를 생성합니다.
     * CSP에서 컨테이너 생성 후 CloudResource 엔티티를 DB에 저장합니다.
     *
     * @param request 생성 요청 정보
     * @return 생성된 Object Storage Container 정보
     */
    @Transactional
    public CloudResource createContainer(CreateObjectStorageContainerRequest request) {
        CloudProvider.ProviderType providerType = Objects.requireNonNull(request.getProviderType(),
                "providerType is required");
        String accountScope = Objects.requireNonNull(request.getAccountScope(), "accountScope is required");
        validateAccountScope(accountScope);
        log.info("[ObjectStorageUseCaseService] createContainer - provider={}, accountScope={}, region={}, containerName={}",
                providerType, accountScope, request.getRegion(), request.getContainerName());

        // getServiceKeyForProvider를 사용하여 일관성 유지
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        log.debug("[ObjectStorageUseCaseService] createContainer - session acquired, expiresAt={}",
                session.getExpiresAt());

        CreateObjectStorageContainerCommand command = CreateObjectStorageContainerCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .containerName(request.getContainerName())
                .region(request.getRegion())
                .tags(request.getTags())
                .objectOwnership(request.getObjectOwnership())
                .objectLockEnabled(request.getObjectLockEnabled())
                .session(session)
                .build();

        // CSP에서 Container 생성
        CloudResource container = router.management(providerType).createContainer(command);

        // DB에 CloudResource 저장
        saveCloudResource(request.getContainerName(), request, providerType);

        log.info("[ObjectStorageUseCaseService] createContainer - success provider={}, containerName={}",
                providerType, request.getContainerName());

        return container;
    }

    /**
     * Object Storage Container 설정을 업데이트합니다.
     *
     * @param request 업데이트 요청 정보
     * @return 업데이트된 Object Storage Container 정보
     */
    @Transactional
    public CloudResource updateContainer(UpdateObjectStorageContainerRequest request) {
        CloudProvider.ProviderType providerType = Objects.requireNonNull(request.getProviderType(),
                "providerType is required");
        String accountScope = Objects.requireNonNull(request.getAccountScope(), "accountScope is required");
        validateAccountScope(accountScope);
        log.info("[ObjectStorageUseCaseService] updateContainer - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, request.getContainerName());

        // getServiceKeyForProvider를 사용하여 일관성 유지
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        log.debug("[ObjectStorageUseCaseService] updateContainer - session acquired, expiresAt={}",
                session.getExpiresAt());

        UpdateObjectStorageContainerCommand command = UpdateObjectStorageContainerCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .containerName(request.getContainerName())
                .versioningEnabled(request.getVersioningEnabled())
                .tags(request.getTags())
                .session(session)
                .build();

        CloudResource container = router.management(providerType).updateContainer(command);

        log.info("[ObjectStorageUseCaseService] updateContainer - success provider={}, containerName={}",
                providerType, request.getContainerName());

        return container;
    }

    /**
     * Object Storage Container를 삭제합니다.
     * CSP에서 컨테이너 삭제 후 DB에서 소프트 삭제 처리합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param containerName Container 이름
     */
    @Transactional
    public void deleteContainer(CloudProvider.ProviderType providerType, String accountScope, String containerName) {
        Objects.requireNonNull(providerType, "providerType is required");
        Objects.requireNonNull(accountScope, "accountScope is required");
        log.info("[ObjectStorageUseCaseService] deleteContainer - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, containerName);

        // getServiceKeyForProvider를 사용하여 일관성 유지
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        log.debug("[ObjectStorageUseCaseService] deleteContainer - session acquired, expiresAt={}",
                session.getExpiresAt());

        // CSP에서 Container 삭제
        router.management(providerType).deleteContainer(session, containerName);

        // DB 소프트 삭제
        softDeleteResourceIfExists(containerName);

        log.info("[ObjectStorageUseCaseService] deleteContainer - success provider={}, containerName={}",
                providerType, containerName);
    }

    /**
     * Object Storage Container를 강제 삭제합니다 (내용물 포함).
     * CSP에서 컨테이너 강제 삭제 후 DB에서 소프트 삭제 처리합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param containerName Container 이름
     */
    @Transactional
    public void forceDeleteContainer(CloudProvider.ProviderType providerType, String accountScope, String containerName) {
        Objects.requireNonNull(providerType, "providerType is required");
        Objects.requireNonNull(accountScope, "accountScope is required");
        log.info("[ObjectStorageUseCaseService] forceDeleteContainer - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, containerName);

        // getServiceKeyForProvider를 사용하여 일관성 유지
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        log.debug("[ObjectStorageUseCaseService] forceDeleteContainer - session acquired, expiresAt={}",
                session.getExpiresAt());

        // CSP에서 Container 강제 삭제
        router.management(providerType).forceDeleteContainer(containerName, session);

        // DB 소프트 삭제
        softDeleteResourceIfExists(containerName);

        log.info("[ObjectStorageUseCaseService] forceDeleteContainer - success provider={}, containerName={}",
                providerType, containerName);
    }

    /**
     * Object Storage Container 목록을 조회합니다.
     *
     * @param query 조회 조건
     * @return Object Storage Container 페이지
     */
    public Page<CloudResource> listContainers(ObjectStorageContainerQueryRequest query) {
        CloudProvider.ProviderType providerType = Objects.requireNonNull(query.getProviderType(),
                "providerType is required");
        String accountScope = query.getAccountScope();
        validateAccountScope(accountScope);
        log.info("[ObjectStorageUseCaseService] listContainers - provider={}, accountScope={}",
                providerType, accountScope);

        // Adapter 내부에서 자격증명(세션)을 획득하여 사용 (패턴 2: Discovery 작업)
        Page<CloudResource> page = router.discovery(providerType).listContainers(query);

        log.info("[ObjectStorageUseCaseService] listContainers - success provider={}, totalElements={}",
                providerType, page.getTotalElements());

        return page;
    }

    /**
     * 특정 Object Storage Container를 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     * @return Object Storage Container 정보
     */
    public CloudResource getContainer(CloudProvider.ProviderType providerType, String accountScope, String containerName) {
        Objects.requireNonNull(providerType, "providerType is required");
        Objects.requireNonNull(accountScope, "accountScope is required");
        validateAccountScope(accountScope);
        log.info("[ObjectStorageUseCaseService] getContainer - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, containerName);

        CloudResource container = router.discovery(providerType).getContainer(accountScope, containerName)
                .orElseThrow(() -> new IllegalArgumentException("Object Storage Container를 찾을 수 없습니다: " + containerName));

        log.info("[ObjectStorageUseCaseService] getContainer - success provider={}, containerName={}",
                providerType, containerName);

        return container;
    }

    /**
     * Object Storage Container 존재 여부를 확인합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     * @return 존재 여부
     */
    public boolean containerExists(CloudProvider.ProviderType providerType, String accountScope, String containerName) {
        Objects.requireNonNull(providerType, "providerType is required");
        Objects.requireNonNull(accountScope, "accountScope is required");
        validateAccountScope(accountScope);
        log.info("[ObjectStorageUseCaseService] containerExists - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, containerName);

        boolean exists = router.discovery(providerType).containerExists(accountScope, containerName);

        log.info("[ObjectStorageUseCaseService] containerExists - success provider={}, containerName={}, exists={}",
                providerType, containerName, exists);

        return exists;
    }

    private void validateAccountScope(String accountScope) {
        if (accountScope == null || accountScope.isBlank()) {
            throw new BusinessException(
                    CloudErrorCode.ACCOUNT_SCOPE_REQUIRED,
                    "AccountScope가 필요합니다."
            );
        }
    }

    // ==================== DB 저장 헬퍼 메서드 ====================

    /**
     * CSP에서 생성된 Object Storage Container 정보를 CloudResource 엔티티로 저장합니다.
     *
     * @param containerName 생성된 컨테이너 이름 (S3 버킷명, Azure Blob 컨테이너명 등)
     * @param request 생성 요청 정보
     * @param providerType 프로바이더 타입
     */
    private void saveCloudResource(String containerName, CreateObjectStorageContainerRequest request, 
                                   ProviderType providerType) {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            
            // Provider 조회
            CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                    .orElseThrow(() -> new IllegalStateException(
                            "CloudProvider not found for type: " + providerType));
            
            // Service 조회 (Storage용 서비스 - S3, BlobStorage, CloudStorage 등)
            CloudService cloudService = cloudServiceRepository
                    .findByProviderTypeAndServiceKey(providerType, getServiceKeyForProvider(providerType))
                    .orElse(null);
            
            // Tenant 조회
            Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Tenant not found for key: " + tenantKey));
            
            // Factory Method를 사용한 CloudResource 엔티티 생성
            CloudResource cloudResource = CloudResource.createStorageBucket(
                    containerName,
                    provider,
                    cloudService,
                    tenant,
                    request.getTags()
            );
            
            cloudResourceRepository.save(cloudResource);
            log.debug("[ObjectStorageUseCaseService] CloudResource 저장 완료: containerName={}", containerName);
            
        } catch (Exception e) {
            // DB 저장 실패해도 CSP 생성은 완료되었으므로 경고 로그만 출력
            log.warn("[ObjectStorageUseCaseService] CloudResource 저장 실패 (CSP 생성은 완료됨): containerName={}, error={}", 
                    containerName, e.getMessage());
        }
    }

    /**
     * 리소스가 존재하면 소프트 삭제 처리합니다.
     *
     * @param resourceId 리소스 ID (containerName)
     */
    private void softDeleteResourceIfExists(String resourceId) {
        try {
            int deletedCount = cloudResourceRepository.softDeleteByResourceId(resourceId);
            
            if (deletedCount > 0) {
                log.debug("[ObjectStorageUseCaseService] 리소스 소프트 삭제 완료: resourceId={}", resourceId);
            } else {
                log.debug("[ObjectStorageUseCaseService] DB에 리소스가 없어 삭제 스킵: resourceId={}", resourceId);
            }
        } catch (Exception e) {
            log.warn("[ObjectStorageUseCaseService] 리소스 소프트 삭제 실패: resourceId={}, error={}", 
                    resourceId, e.getMessage());
        }
    }

    /**
     * 프로바이더 타입에 따른 서비스 키 반환
     * AWS: S3, Azure: BlobStorage, GCP: CloudStorage 등
     */
    private String getServiceKeyForProvider(ProviderType providerType) {
        return switch (providerType) {
            case AWS -> "S3";
            case AZURE -> "BlobStorage";
            case GCP -> "CloudStorage";
            default -> "ObjectStorage";
        };
    }
}
