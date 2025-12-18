package com.agenticcp.core.domain.cloud.service.rdbms;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.RdbmsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsUpdateRequest;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsQuery;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsUpdateCommand;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * RDBMS 유스케이스 서비스
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 RDBMS 인스턴스 관련 비즈니스 로직을 처리합니다.
 * 포트 인터페이스를 통해서만 외부 시스템과 통신하며, 트랜잭션을 담당합니다.
 * 
 * JIT 세션 관리 패턴을 따릅니다:
 * - 모든 작업에서 Service 레벨에서 세션을 획득하여 Port에 전달
 * - getSession()을 통한 Redis 캐싱 활용
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdbmsUseCaseService {

    /**
     * RDBMS 리소스 타입
     */
    private static final String RESOURCE_TYPE = "DATABASE";

    private final RdbmsPortRouter portRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort credentialPort;
    private final CloudResourceManagementHelper resourceHelper;

    /**
     * 세션 자격증명을 획득합니다.
     * JIT 세션 관리 패턴을 따릅니다.
     * - Redis 캐시 확인 → 없으면 발급 → 캐싱 → 반환
     * 
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @return CloudSessionCredential 세션 자격증명
     */
    private CloudSessionCredential getSession(ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        return credentialPort.getSession(tenantKey, accountScope, providerType);
    }

    // ==================== 인스턴스 조회 ====================

    /**
     * RDBMS 인스턴스 목록을 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param request 조회 요청
     * @return CloudResource 페이지
     */
    @Transactional(readOnly = true)
    public Page<CloudResource> listRdbmsInstances(ProviderType providerType, String accountScope, RdbmsQueryRequest request) {
        log.debug("RDBMS 인스턴스 목록 조회 시작: provider={}, accountScope={}, request={}", 
                providerType, accountScope, request);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Query 변환
        RdbmsQuery query = toQuery(providerType, accountScope, request);

        Page<CloudResource> result = portRouter.discovery(providerType).listRdbmsInstances(query, session);

        log.info("RDBMS 인스턴스 목록 조회 완료: provider={}, totalElements={}", providerType, result.getTotalElements());
        return result;
    }

    /**
     * 특정 RDBMS 인스턴스를 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public Optional<CloudResource> getRdbmsInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("RDBMS 인스턴스 조회 시작: provider={}, accountScope={}, instanceId={}", 
                providerType, accountScope, instanceId);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        Optional<CloudResource> result = portRouter.discovery(providerType).getRdbmsInstance(instanceId, session);

        log.info("RDBMS 인스턴스 조회 완료: provider={}, instanceId={}, found={}", 
                providerType, instanceId, result.isPresent());
        return result;
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 RDBMS 인스턴스를 생성합니다.
     * CSP에서 인스턴스 생성 후 CloudResource 엔티티를 DB에 저장합니다.
     * 
     * 보상 트랜잭션: DB 저장 실패 시 CSP에 생성된 인스턴스를 삭제하여
     * 데이터 정합성(Ghost Resource 방지)을 보장합니다.
     *
     * @param request 생성 요청 정보 (providerType, accountScope 포함)
     * @return 생성된 CloudResource 엔티티
     * @throws BusinessException DB 저장 실패 및 보상 트랜잭션 실행 시
     */
    @Transactional
    public CloudResource createRdbms(RdbmsCreateRequest request) {
        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();
        
        log.debug("RDBMS 인스턴스 생성 시작: provider={}, accountScope={}, request={}", 
                providerType, accountScope, request);

        // Capability 검증 (CSP별 실제 서비스 키 사용)
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.CREATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Command 변환
        RdbmsCreateCommand command = toCreateCommand(request, session);

        // CSP에서 RDBMS 인스턴스 생성
        CloudResource resource = portRouter.management(providerType).createRdbms(command);

        // DB에 CloudResource 저장 (실패 시 보상 트랜잭션 실행)
        CloudResource savedResource;
        try {
            // Adapter에서 이미 CloudResource를 반환하므로, ID로 조회하여 저장
            // 또는 Adapter에서 이미 저장된 경우 그대로 반환
            savedResource = resource;
            
            log.info("RDBMS 인스턴스 생성 완료: provider={}, instanceId={}, resourceId={}", 
                    providerType, resource.getResourceId(), resource.getId());
        } catch (Exception e) {
            log.error("[RdbmsUseCaseService] DB 저장 실패, 보상 트랜잭션 실행: instanceId={}, error={}",
                    resource.getResourceId(), e.getMessage());
            
            // 보상 트랜잭션: CSP에 생성된 인스턴스 삭제
            executeCompensatingTransaction(providerType, accountScope, session, resource.getResourceId());
            
            throw new BusinessException(
                    CloudErrorCode.RESOURCE_CREATION_FAILED,
                    "RDBMS 인스턴스 생성 후 DB 저장 실패로 인해 롤백되었습니다: " + resource.getResourceId()
            );
        }

        return savedResource;
    }

    /**
     * 보상 트랜잭션: CSP에 생성된 RDBMS 인스턴스를 삭제합니다.
     * Ghost Resource 방지를 위해 DB 저장 실패 시 호출됩니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param session      세션 자격증명
     * @param instanceId   삭제할 인스턴스 ID
     */
    private void executeCompensatingTransaction(
            ProviderType providerType,
            String accountScope,
            CloudSessionCredential session,
            String instanceId
    ) {
        try {
            log.warn("[RdbmsUseCaseService] 보상 트랜잭션 실행: CSP 인스턴스 삭제 시도 - instanceId={}", instanceId);
            
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            
            RdbmsDeleteCommand deleteCommand = RdbmsDeleteCommand.builder()
                    .providerType(providerType)
                    .accountScope(accountScope)
                    .region(null) // region은 Adapter에서 처리
                    .providerResourceId(instanceId)
                    .skipSnapshot(true) // 보상 트랜잭션이므로 스냅샷 생성하지 않음
                    .deleteAutomatedBackups(false)
                    .tenantKey(tenantKey)
                    .session(session)
                    .build();
            
            portRouter.management(providerType).deleteRdbms(deleteCommand);
            log.info("[RdbmsUseCaseService] 보상 트랜잭션 완료: CSP 인스턴스 삭제 성공 - instanceId={}", instanceId);
        } catch (Exception compensationError) {
            // 보상 트랜잭션도 실패한 경우 - Ghost Resource 발생
            // 이 경우 별도의 모니터링/알림 시스템이나 배치 동기화로 처리 필요
            log.error("[RdbmsUseCaseService] 보상 트랜잭션 실패: Ghost Resource 발생 가능 - instanceId={}, error={}",
                    instanceId, compensationError.getMessage());
        }
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * RDBMS 인스턴스 정보를 수정합니다.
     *
     * @param request 수정 요청 정보 (providerType, accountScope 포함)
     * @return 수정된 CloudResource 엔티티
     */
    @Transactional
    public CloudResource updateRdbms(RdbmsUpdateRequest request) {
        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();
        
        log.debug("RDBMS 인스턴스 수정: provider={}, accountScope={}, request={}", 
                providerType, accountScope, request);

        // Capability 검증 (CSP별 실제 서비스 키 사용)
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.UPDATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Command 변환
        RdbmsUpdateCommand command = toUpdateCommand(request, session);

        // RDBMS 인스턴스 수정
        CloudResource resource = portRouter.management(providerType).updateRdbms(command);

        log.info("RDBMS 인스턴스 수정 완료: provider={}, instanceId={}", providerType, request.getInstanceId());
        return resource;
    }

    // ==================== 인스턴스 삭제 ====================

    /**
     * RDBMS 인스턴스를 삭제합니다.
     * CSP에서 인스턴스 삭제 후 DB에서 소프트 삭제 처리합니다.
     *
     * @param request 삭제 요청 정보 (providerType, accountScope 포함)
     */
    @Transactional
    public void deleteRdbms(RdbmsDeleteRequest request) {
        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();
        String instanceId = request.getInstanceId();
        
        log.debug("RDBMS 인스턴스 삭제: provider={}, accountScope={}, request={}", 
                providerType, accountScope, request);

        // Capability 검증 (CSP별 실제 서비스 키 사용)
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Command 변환
        RdbmsDeleteCommand command = toDeleteCommand(request, session);

        // CSP에서 RDBMS 인스턴스 삭제
        portRouter.management(providerType).deleteRdbms(command);

        // DB 소프트 삭제
        resourceHelper.softDeleteResource(instanceId);

        log.info("RDBMS 인스턴스 삭제 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * RDBMS 인스턴스를 시작합니다.
     * CSP에서 인스턴스 시작 후 DB의 lifecycleState를 RUNNING으로 업데이트합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void startInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("RDBMS 인스턴스 시작: provider={}, accountScope={}, instanceId={}", 
                providerType, accountScope, instanceId);

        // Capability 검증 (CSP별 실제 서비스 키 사용)
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.START);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // ResourceIdentity 생성 (region 정보는 인스턴스 조회를 통해 얻거나 null로 설정)
        String region = getRegionFromInstance(providerType, accountScope, instanceId, session);
        ResourceIdentity resourceId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(instanceId)
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .build();

        // CSP에서 RDBMS 인스턴스 시작 (RdbmsLifecyclePort는 ResourceLifecyclePort를 확장)
        portRouter.lifecycle(providerType).start(resourceId, session);

        // DB 상태 업데이트: RUNNING
        resourceHelper.updateLifecycleState(instanceId, LifecycleState.RUNNING);

        log.info("RDBMS 인스턴스 시작 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * RDBMS 인스턴스를 중지합니다.
     * CSP에서 인스턴스 중지 후 DB의 lifecycleState를 STOPPED로 업데이트합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void stopInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("RDBMS 인스턴스 중지: provider={}, accountScope={}, instanceId={}", 
                providerType, accountScope, instanceId);

        // Capability 검증 (CSP별 실제 서비스 키 사용)
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // ResourceIdentity 생성 (region 정보는 인스턴스 조회를 통해 얻거나 null로 설정)
        String region = getRegionFromInstance(providerType, accountScope, instanceId, session);
        ResourceIdentity resourceId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(instanceId)
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .build();

        // CSP에서 RDBMS 인스턴스 중지 (RdbmsLifecyclePort는 ResourceLifecyclePort를 확장)
        portRouter.lifecycle(providerType).stop(resourceId, session);

        // DB 상태 업데이트: STOPPED
        resourceHelper.updateLifecycleState(instanceId, LifecycleState.STOPPED);

        log.info("RDBMS 인스턴스 중지 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * RDBMS 인스턴스를 재시작합니다.
     * CSP에서 인스턴스 재시작 후 DB의 lifecycleState를 RUNNING으로 유지합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void rebootInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("RDBMS 인스턴스 재시작: provider={}, accountScope={}, instanceId={}", 
                providerType, accountScope, instanceId);

        // Capability 검증 (START와 STOP이 모두 필요, CSP별 실제 서비스 키 사용)
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.START);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // CSP에서 RDBMS 인스턴스 재시작
        portRouter.lifecycle(providerType).rebootInstance(instanceId, session);

        // DB 상태 업데이트: 재시작 후 RUNNING 상태 유지
        resourceHelper.updateLifecycleState(instanceId, LifecycleState.RUNNING);

        log.info("RDBMS 인스턴스 재시작 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    // ==================== 상태 확인 ====================

    /**
     * RDBMS 인스턴스의 현재 상태를 확인합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    @Transactional(readOnly = true)
    public String getInstanceStatus(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("RDBMS 인스턴스 상태 확인: provider={}, accountScope={}, instanceId={}", 
                providerType, accountScope, instanceId);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        String status = portRouter.discovery(providerType).getInstanceStatus(instanceId, session);

        log.info("RDBMS 인스턴스 상태 확인 완료: provider={}, instanceId={}, status={}", 
                providerType, instanceId, status);
        return status;
    }

    // ==================== Helper Methods ====================

    /**
     * 인스턴스 ID로부터 region 정보를 조회합니다.
     * 인스턴스가 존재하지 않으면 null을 반환합니다.
     */
    private String getRegionFromInstance(ProviderType providerType, String accountScope, 
                                         String instanceId, CloudSessionCredential session) {
        try {
            Optional<CloudResource> resource = portRouter.discovery(providerType)
                    .getRdbmsInstance(instanceId, session);
            return resource
                    .map(r -> r.getRegion() != null ? r.getRegion().getRegionKey() : null)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get region for instance {}: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 프로바이더 타입에 따른 서비스 키 반환
     * AWS: RDS, Azure: AzureDatabase, GCP: CloudSQL 등
     */
    private String getServiceKeyForProvider(ProviderType providerType) {
        return switch (providerType) {
            case AWS -> "RDS";
            case AZURE -> "AzureDatabase";
            case GCP -> "CloudSQL";
            default -> "RDBMS";
        };
    }

    // ==================== Command 변환 ====================

    private RdbmsQuery toQuery(ProviderType providerType, String accountScope, RdbmsQueryRequest request) {
        return RdbmsQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .regions(request.getRegions())
                .instanceName(request.getInstanceName())
                .engine(request.getEngine())
                .instanceSize(request.getInstanceSize())
                .status(request.getStatus())
                .tagsEquals(request.getTags())
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }

    private RdbmsCreateCommand toCreateCommand(RdbmsCreateRequest request, CloudSessionCredential session) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        String serviceKey = getServiceKeyForProvider(request.getProviderType());
        
        return RdbmsCreateCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(request.getAccountScope())
                .region(request.getRegion())
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .instanceName(request.getInstanceName())
                .engine(request.getEngine())
                .engineVersion(request.getEngineVersion())
                .instanceSize(request.getInstanceSize())
                .allocatedStorage(request.getAllocatedStorage())
                .adminUsername(request.getMasterUsername())
                .adminPassword(request.getMasterPassword())
                .dbName(request.getDbName())
                .networkSecurityId(request.getNetworkSecurityId())
                .subnetId(request.getSubnetId())
                .port(request.getPort())
                .zone(request.getZone())
                .highAvailability(request.getHighAvailability())
                .publiclyAccessible(request.getPubliclyAccessible())
                .tags(request.getTags())
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();
    }

    private RdbmsUpdateCommand toUpdateCommand(RdbmsUpdateRequest request, CloudSessionCredential session) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // tagsToAdd를 tags로 사용 (tagsToRemove는 Adapter에서 처리하거나 별도 필드로 전달 필요)
        // 현재는 tagsToAdd만 전달 (실제 구현에서는 Adapter에서 태그 추가/제거를 별도로 처리)
        java.util.Map<String, String> tags = request.getTagsToAdd() != null 
                ? new java.util.HashMap<>(request.getTagsToAdd()) 
                : new java.util.HashMap<>();
        
        return RdbmsUpdateCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(request.getAccountScope())
                .region(null) // region은 Adapter에서 처리
                .providerResourceId(request.getInstanceId())
                .instanceSize(request.getInstanceSize())
                .allocatedStorage(request.getAllocatedStorage())
                .adminPassword(request.getMasterPassword())
                .applyImmediately(request.getApplyImmediately())
                .tags(tags)
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();
    }

    private RdbmsDeleteCommand toDeleteCommand(RdbmsDeleteRequest request, CloudSessionCredential session) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        return RdbmsDeleteCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(request.getAccountScope())
                .region(null) // region은 Adapter에서 처리
                .providerResourceId(request.getInstanceId())
                .skipSnapshot(request.getSkipSnapshot())
                .snapshotName(request.getSnapshotName())
                .deleteAutomatedBackups(request.getDeleteAutomatedBackups())
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();
    }

}
