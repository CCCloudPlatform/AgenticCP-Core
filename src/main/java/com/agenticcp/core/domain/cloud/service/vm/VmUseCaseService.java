package com.agenticcp.core.domain.cloud.service.vm;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmUpdateCommand;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가상머신(VM) 유스케이스 서비스
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 VM 인스턴스 관련 비즈니스 로직을 처리합니다.
 * 포트 인터페이스를 통해서만 외부 시스템과 통신하며, 트랜잭션을 담당합니다.
 * 
 * JIT 세션 관리 패턴을 따릅니다:
 * - 모든 작업에서 Service 레벨에서 세션을 획득하여 Port에 전달
 * - getSession()을 통한 Redis 캐싱 활용
 *
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VmUseCaseService {

    private static final String SERVICE_KEY = "VM";
    private static final String RESOURCE_TYPE = "INSTANCE";

    private final VmPortRouter vmPortRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort credentialProviderPort;

    /**
     * 세션 자격증명을 획득합니다.
     * IT 세션 관리 패턴을 따릅니다.
     * - Redis 캐시 확인 → 없으면 발급 → 캐싱 → 반환
     * 
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @return CloudSessionCredential 세션 자격증명
     */
    private CloudSessionCredential getSession(ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        return credentialProviderPort.getSession(tenantKey, accountScope, providerType);
    }

    // ==================== 인스턴스 조회 ====================

    /**
     * VM 인스턴스 목록을 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param query 조회 조건
     * @return CloudResource 페이지
     */
    public Page<CloudResource> listInstances(ProviderType providerType, String accountScope, VmQuery query) {
        log.debug("VM 인스턴스 목록 조회 시작: provider={}, accountScope={}, query={}", providerType, accountScope, query);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        Page<CloudResource> result = vmPortRouter.discovery(providerType).listInstances(query, session);

        log.info("VM 인스턴스 목록 조회 완료: provider={}, totalElements={}", providerType, result.getTotalElements());
        return result;
    }

    /**
     * 특정 VM 인스턴스를 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    public Optional<CloudResource> getInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 조회 시작: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        Optional<CloudResource> result = vmPortRouter.discovery(providerType).getInstance(instanceId, session);

        log.info("VM 인스턴스 조회 완료: provider={}, instanceId={}, found={}", providerType, instanceId, result.isPresent());
        return result;
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 VM 인스턴스를 생성합니다.
     *
     * @param request 생성 요청 정보 (providerType, accountScope 포함)
     * @return 생성된 인스턴스 ID
     */
    @Transactional
    public String createInstance(VmCreateRequest request) {
        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();
        
        log.debug("VM 인스턴스 생성 시작: provider={}, accountScope={}, request={}", providerType, accountScope, request);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.CREATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 생성
        String instanceId = vmPortRouter.lifecycle(providerType)
            .createInstance(toCreateCommand(request, session));

        log.info("VM 인스턴스 생성 완료: provider={}, instanceId={}", providerType, instanceId);
        return instanceId;
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * VM 인스턴스를 시작합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void startInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 시작: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.START);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 시작
        vmPortRouter.lifecycle(providerType).startInstance(instanceId, session);

        log.info("VM 인스턴스 시작 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * VM 인스턴스를 중지합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void stopInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 중지: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 중지
        vmPortRouter.lifecycle(providerType).stopInstance(instanceId, session);

        log.info("VM 인스턴스 중지 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * VM 인스턴스를 재부팅합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void rebootInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 재부팅: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.START);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 재부팅
        vmPortRouter.lifecycle(providerType).rebootInstance(instanceId, session);

        log.info("VM 인스턴스 재부팅 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * VM 인스턴스를 종료합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void terminateInstance(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 종료: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 종료
        vmPortRouter.lifecycle(providerType).terminateInstance(instanceId, session);

        log.info("VM 인스턴스 종료 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * VM 인스턴스를 삭제합니다.
     *
     * @param request 삭제 요청 정보 (providerType, accountScope 포함)
     */
    @Transactional
    public void deleteInstance(VmDeleteRequest request) {
        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();
        
        log.debug("VM 인스턴스 삭제: provider={}, accountScope={}, request={}", providerType, accountScope, request);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 삭제
        vmPortRouter.lifecycle(providerType).deleteInstance(toDeleteCommand(request, session));

        log.info("VM 인스턴스 삭제 완료: provider={}, instanceId={}", providerType, request.getInstanceId());
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * VM 인스턴스 정보를 수정합니다.
     *
     * @param request 수정 요청 정보 (providerType, accountScope 포함)
     */
    @Transactional
    public void updateInstance(VmUpdateRequest request) {
        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();
        
        log.debug("VM 인스턴스 수정: provider={}, accountScope={}, request={}", providerType, accountScope, request);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.UPDATE);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // VM 인스턴스 수정
        vmPortRouter.lifecycle(providerType).updateInstance(toUpdateCommand(request, session));

        log.info("VM 인스턴스 수정 완료: provider={}, instanceId={}", providerType, request.getInstanceId());
    }

    // ==================== 태그 관리 ====================

    /**
     * VM 인스턴스에 태그를 추가합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     */
    @Transactional
    public void addTags(ProviderType providerType, String accountScope, String instanceId, Map<String, String> tags) {
        log.debug("VM 인스턴스 태그 추가: provider={}, accountScope={}, instanceId={}, tags={}", 
            providerType, accountScope, instanceId, tags);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // 태그 추가
        vmPortRouter.tagging(providerType).addTags(instanceId, tags, session);

        log.info("VM 인스턴스 태그 추가 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * VM 인스턴스에서 태그를 제거합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     */
    @Transactional
    public void removeTags(ProviderType providerType, String accountScope, String instanceId, Map<String, String> tagKeys) {
        log.debug("VM 인스턴스 태그 제거: provider={}, accountScope={}, instanceId={}, tagKeys={}", 
            providerType, accountScope, instanceId, tagKeys.keySet());

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // 태그 제거
        vmPortRouter.tagging(providerType).removeTags(instanceId, tagKeys, session);

        log.info("VM 인스턴스 태그 제거 완료: provider={}, instanceId={}", providerType, instanceId);
    }

    /**
     * VM 인스턴스의 모든 태그를 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 태그 맵
     */
    public Map<String, String> getTags(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 태그 조회: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        Map<String, String> tags = vmPortRouter.tagging(providerType).getTags(instanceId, session);

        log.info("VM 인스턴스 태그 조회 완료: provider={}, instanceId={}, tagCount={}", 
            providerType, instanceId, tags.size());
        return tags;
    }

    // ==================== 상태 확인 ====================

    /**
     * VM 인스턴스의 현재 상태를 확인합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    public String getInstanceStatus(ProviderType providerType, String accountScope, String instanceId) {
        log.debug("VM 인스턴스 상태 확인: provider={}, accountScope={}, instanceId={}", providerType, accountScope, instanceId);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        String status = vmPortRouter.discovery(providerType).getInstanceStatus(instanceId, session);

        log.info("VM 인스턴스 상태 확인 완료: provider={}, instanceId={}, status={}", providerType, instanceId, status);
        return status;
    }

    /**
     * VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    public boolean waitForInstanceStatus(ProviderType providerType, String accountScope, String instanceId, 
                                         String targetStatus, int timeoutSeconds) {
        log.debug("VM 인스턴스 상태 대기: provider={}, accountScope={}, instanceId={}, targetStatus={}, timeout={}s", 
            providerType, accountScope, instanceId, targetStatus, timeoutSeconds);

        // 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        boolean success = vmPortRouter.discovery(providerType)
            .waitForInstanceStatus(instanceId, targetStatus, timeoutSeconds, session);

        log.info("VM 인스턴스 상태 대기 완료: provider={}, instanceId={}, success={}", providerType, instanceId, success);
        return success;
    }

    // ==================== Command 변환 ====================

    private VmCreateCommand toCreateCommand(VmCreateRequest request, CloudSessionCredential session) {
        return VmCreateCommand.builder()
                .image(request.getImage())
                .instanceSize(request.getInstanceSize())
                .sshKey(request.getSshKey())
                .networkSecurityId(request.getNetworkSecurityId())
                .subnetId(request.getSubnetId())
                .zone(request.getZone())
                .userData(request.getUserData())
                .tags(request.getTags())
                .minCount(request.getMinCount())
                .maxCount(request.getMaxCount())
                .session(session)
                .build();
    }

    private VmUpdateCommand toUpdateCommand(VmUpdateRequest request, CloudSessionCredential session) {
        return VmUpdateCommand.builder()
                .instanceId(request.getInstanceId())
                .instanceType(request.getInstanceType())
                .userData(request.getUserData())
                .tagsToAdd(request.getTagsToAdd())
                .tagsToRemove(request.getTagsToRemove())
                .session(session)
                .build();
    }

    private VmDeleteCommand toDeleteCommand(VmDeleteRequest request, CloudSessionCredential session) {
        return VmDeleteCommand.builder()
                .instanceId(request.getInstanceId())
                .force(request.isForce())
                .reason(request.getReason())
                .createSnapshot(request.isCreateSnapshot())
                .session(session)
                .build();
    }
}
