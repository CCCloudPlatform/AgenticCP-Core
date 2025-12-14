package com.agenticcp.core.domain.cloud.service.storage;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.CreateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.dto.ObjectStorageContainerQueryRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.dto.UpdateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.CreateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.model.storage.UpdateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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
    private final CloudResourceManagementHelper resourceHelper;

    private static final String RESOURCE_TYPE = "BUCKET";

    /**
     * Object Storage Container를 생성합니다.
     * CSP에서 컨테이너 생성 후 CloudResource 엔티티를 DB에 저장합니다.
     * 
     * 보상 트랜잭션: DB 저장 실패 시 CSP에 생성된 컨테이너를 삭제하여
     * 데이터 정합성(Ghost Resource 방지)을 보장합니다.
     *
     * @param request 생성 요청 정보
     * @return 생성된 Object Storage Container 정보
     * @throws BusinessException DB 저장 실패 및 보상 트랜잭션 실행 시
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

        // DB에 CloudResource 저장 (실패 시 보상 트랜잭션 실행)
        try {
            ResourceRegistrationRequest registrationRequest = ResourceRegistrationRequest.builder()
                    .resourceId(request.getContainerName())
                    .resourceName(request.getContainerName())
                    .resourceType(CloudResource.ResourceType.BUCKET)
                    .tags(request.getTags())
                    .build();
            
            resourceHelper.registerResource(
                    providerType,
                    serviceKey,
                    registrationRequest
            );
        } catch (Exception e) {
            log.error("[ObjectStorageUseCaseService] DB 저장 실패, 보상 트랜잭션 실행: containerName={}, error={}",
                    request.getContainerName(), e.getMessage());
            
            // 보상 트랜잭션: CSP에 생성된 컨테이너 삭제
            executeCompensatingTransaction(providerType, session, request.getContainerName());
            
            throw new BusinessException(
                    CloudErrorCode.RESOURCE_CREATION_FAILED,
                    "컨테이너 생성 후 DB 저장 실패로 인해 롤백되었습니다: " + request.getContainerName()
            );
        }

        log.info("[ObjectStorageUseCaseService] createContainer - success provider={}, containerName={}",
                providerType, request.getContainerName());

        return container;
    }

    /**
     * 보상 트랜잭션: CSP에 생성된 컨테이너를 삭제합니다.
     * Ghost Resource 방지를 위해 DB 저장 실패 시 호출됩니다.
     *
     * @param providerType  프로바이더 타입
     * @param session       세션 자격증명
     * @param containerName 삭제할 컨테이너 이름
     */
    private void executeCompensatingTransaction(
            ProviderType providerType,
            CloudSessionCredential session,
            String containerName
    ) {
        try {
            log.warn("[ObjectStorageUseCaseService] 보상 트랜잭션 실행: CSP 컨테이너 삭제 시도 - containerName={}", 
                    containerName);
            router.management(providerType).deleteContainer(session, containerName);
            log.info("[ObjectStorageUseCaseService] 보상 트랜잭션 완료: CSP 컨테이너 삭제 성공 - containerName={}", 
                    containerName);
        } catch (Exception compensationError) {
            // 보상 트랜잭션도 실패한 경우 - Ghost Resource 발생
            // 이 경우 별도의 모니터링/알림 시스템이나 배치 동기화로 처리 필요
            log.error("[ObjectStorageUseCaseService] 보상 트랜잭션 실패: Ghost Resource 발생 가능 - containerName={}, error={}",
                    containerName, compensationError.getMessage());
        }
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
        resourceHelper.softDeleteResource(containerName);

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
        resourceHelper.softDeleteResource(containerName);

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

    // ==================== Private Helper Methods ====================

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
