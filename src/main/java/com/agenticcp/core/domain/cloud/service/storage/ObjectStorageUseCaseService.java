package com.agenticcp.core.domain.cloud.service.storage;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.*;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
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

    /**
     * Object Storage Container를 생성합니다.
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

        capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);

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

        CloudResource container = router.management(providerType).createContainer(command);

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

        capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);

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
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     */
    @Transactional
    public void deleteContainer(CloudProvider.ProviderType providerType, String accountScope, String containerName) {
        Objects.requireNonNull(providerType, "providerType is required");
        Objects.requireNonNull(accountScope, "accountScope is required");
        log.info("[ObjectStorageUseCaseService] deleteContainer - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, containerName);

        capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        log.debug("[ObjectStorageUseCaseService] deleteContainer - session acquired, expiresAt={}",
                session.getExpiresAt());

        router.management(providerType).deleteContainer(session, containerName);

        log.info("[ObjectStorageUseCaseService] deleteContainer - success provider={}, containerName={}",
                providerType, containerName);
    }

    /**
     * Object Storage Container를 강제 삭제합니다 (내용물 포함).
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     */
    @Transactional
    public void forceDeleteContainer(CloudProvider.ProviderType providerType, String accountScope, String containerName) {
        Objects.requireNonNull(providerType, "providerType is required");
        Objects.requireNonNull(accountScope, "accountScope is required");
        log.info("[ObjectStorageUseCaseService] forceDeleteContainer - provider={}, accountScope={}, containerName={}",
                providerType, accountScope, containerName);

        capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        log.debug("[ObjectStorageUseCaseService] forceDeleteContainer - session acquired, expiresAt={}",
                session.getExpiresAt());

        router.management(providerType).forceDeleteContainer(containerName, session);

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
}
