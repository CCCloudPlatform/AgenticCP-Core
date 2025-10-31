package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.storage.*;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CredentialProviderPort credentialProviderPort;
    
    /**
     * 테넌트 컨텍스트 설정 및 자격증명 해결
     */
    private void setupTenantContextAndCredentials(CloudProvider.ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKey();
        credentialProviderPort.resolveCredentials(tenantKey, providerType, accountScope);
    }

    /**
     * Object Storage Container를 생성합니다.
     *
     * @return 생성된 Object Storage Container 정보
     */
    @Transactional
    public CloudResource createContainer(CloudProvider.ProviderType providerType, CreateObjectStorageContainerRequest request) {
            log.debug("Object Storage Container 생성 시작: provider={}, request={}", providerType, request);

            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, request.getRegion());

            // Capability 검증
            capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);

            CreateObjectStorageContainerCommand command = CreateObjectStorageContainerCommand.builder()
                    .containerName(request.getContainerName())
                    .region(request.getRegion())
                    .tags(request.getTags())
                    .objectOwnership(request.getObjectOwnership())
                    .objectLockEnabled(request.getObjectLockEnabled())
                    .build();

            CloudResource container = router.management(providerType).createContainer(command);

            log.info("Object Storage Container 생성 완료: provider={}, containerName={}", providerType, request.getContainerName());

            return container;
    }

    /**
     * Object Storage Container 설정을 업데이트합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     * @return 업데이트된 Object Storage Container 정보
     */
    @Transactional
    public CloudResource updateContainer(CloudProvider.ProviderType providerType, String containerName,
                                      UpdateObjectStorageContainerRequest request) {

        log.debug("Object Storage Container 업데이트 시작: provider={}, containerName={}, request={}", providerType, containerName, request);

        // 테넌트 컨텍스트 설정 및 자격증명 해결
        setupTenantContextAndCredentials(providerType, null);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);

        // Object Storage Container 업데이트
        UpdateObjectStorageContainerCommand command = UpdateObjectStorageContainerCommand.builder()
                .containerName(containerName)
                .versioningEnabled(request.getVersioningEnabled())
                .tags(request.getTags())
                .build();

        CloudResource container = router.management(providerType).updateContainer(command);

        log.info("Object Storage Container 업데이트 완료: provider={}, containerName={}", providerType, containerName);

        return container;
    }

    /**
     * Object Storage Container 목록을 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param query 조회 조건
     * @return Object Storage Container 페이지
     */
    public Page<CloudResource> listContainers(CloudProvider.ProviderType providerType, ObjectStorageContainerQuery query) {
            log.debug("Object Storage Container 목록 조회 시작: provider={}, query={}", providerType, query);

            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);

            // Object Storage Container 목록 조회
            Page<CloudResource> page = router.discovery(providerType).listContainers(query);
            log.info("Object Storage Container 목록 조회 완료: provider={}, totalElements={}",
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
    public CloudResource getContainer(CloudProvider.ProviderType providerType, String containerName) {
            log.debug("Object Storage Container 조회 시작: provider={}, containerName={}", providerType, containerName);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // Object Storage Container 조회
            CloudResource container = router.discovery(providerType).getContainer(containerName)
                    .orElseThrow(() -> new IllegalArgumentException("Object Storage Container를 찾을 수 없습니다: " + containerName));
            
            log.info("Object Storage Container 조회 완료: provider={}, containerName={}", providerType, containerName);

            return container;
    }

    /**
     * Object Storage Container 존재 여부를 확인합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     * @return 존재 여부
     */
    public boolean containerExists(CloudProvider.ProviderType providerType, String containerName) {
            log.debug("Object Storage Container 존재 확인 시작: provider={}, containerName={}", providerType, containerName);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // Object Storage Container 존재 확인
            boolean exists = router.discovery(providerType).containerExists(containerName);
            
            log.info("Object Storage Container 존재 확인 완료: provider={}, containerName={}, exists={}", 
                    providerType, containerName, exists);
            
            return exists;
    }

    /**
     * Object Storage Container를 삭제합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     */
    @Transactional
    public void deleteContainer(CloudProvider.ProviderType providerType, String containerName) {
            log.debug("Object Storage Container 삭제 시작: provider={}, containerName={}", providerType, containerName);
            
            // Capability 검증
            capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // Object Storage Container 삭제
            router.management(providerType).deleteContainer(containerName);
            
            log.info("Object Storage Container 삭제 완료: provider={}, containerName={}", providerType, containerName);
    }

    /**
     * Object Storage Container를 강제 삭제합니다 (내용물 포함).
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param containerName Container 이름
     */
    @Transactional
    public void forceDeleteContainer(CloudProvider.ProviderType providerType, String containerName) {
            log.debug("Object Storage Container 강제 삭제 시작: provider={}, containerName={}", providerType, containerName);
            
            // Capability 검증
            capabilityGuard.ensureSupported(providerType, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // Object Storage Container 강제 삭제
            router.management(providerType).forceDeleteContainer(containerName);
            
            log.info("Object Storage Container 강제 삭제 완료: provider={}, containerName={}", providerType, containerName);
    }
}
