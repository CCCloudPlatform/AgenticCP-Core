package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import org.springframework.beans.factory.annotation.Autowired;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * 가상머신(VM) 유스케이스 서비스
 *
 * VM 인스턴스 관리에 대한 비즈니스 로직을 담당하며,
 * 핵사고날 아키텍처의 애플리케이션 계층에서 포트를 통해 외부 시스템과 통신합니다.
 *
 * 주요 기능:
 * - VM 인스턴스 조회, 생성, 수정, 삭제
 * - 인스턴스 생명주기 관리 (시작, 중지, 재부팅, 종료)
 * - 태그 관리 및 상태 확인
 * - 감사 로그 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VmUseCaseService {

    private final VmPortRouter vmPortRouter;
    private final AuditEventPort auditEventPort;
    @Autowired(required = false)
    private CapabilityGuard capabilityGuard;

    private static final String SERVICE_KEY = "VM";
    private static final String RESOURCE_TYPE = "INSTANCE";

    // ==================== 인스턴스 조회 ====================

    /**
     * VM 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건
     * @return CloudResource 페이지
     */
    public Page<CloudResource> listInstances(VmQuery query) {
        log.info("[VmUseCaseService] listInstances - query={}", query);

        try {
            Page<CloudResource> result = vmPortRouter.vm(ProviderType.AWS)
                .listInstances(query);

            auditEventPort.record("LIST_INSTANCES", "VM", "SUCCESS",
                Map.of("count", result.getTotalElements(), "query", query));

            log.info("[VmUseCaseService] listInstances - success count={}", result.getTotalElements());
            return result;

        } catch (Exception e) {
            log.error("[VmUseCaseService] listInstances - failed", e);
            auditEventPort.record("LIST_INSTANCES", "VM", "FAILED",
                Map.of("error", e.getMessage(), "query", query));
            throw e;
        }
    }

    /**
     * 특정 VM 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    public Optional<CloudResource> getInstance(String instanceId) {
        log.info("[VmUseCaseService] getInstance - instanceId={}", instanceId);

        try {
            Optional<CloudResource> result = vmPortRouter.vm(ProviderType.AWS)
                .getInstance(instanceId);

            auditEventPort.record("GET_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", instanceId, "found", result.isPresent()));

            log.info("[VmUseCaseService] getInstance - success found={}", result.isPresent());
            return result;

        } catch (Exception e) {
            log.error("[VmUseCaseService] getInstance - failed", e);
            auditEventPort.record("GET_INSTANCE", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 VM 인스턴스를 생성합니다.
     * 
     * @param request 생성 요청 정보
     * @return 생성된 인스턴스 ID
     */
    @Transactional
    public String createInstance(VmCreateRequest request) {
        log.info("[VmUseCaseService] createInstance - request={}", request);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.CREATE);
            }
            String instanceId = vmPortRouter.vm(ProviderType.AWS)
                .createInstance(request);

            auditEventPort.record("CREATE_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", instanceId, "request", request));

            log.info("[VmUseCaseService] createInstance - success instanceId={}", instanceId);
            return instanceId;

        } catch (Exception e) {
            log.error("[VmUseCaseService] createInstance - failed", e);
            auditEventPort.record("CREATE_INSTANCE", "VM", "FAILED",
                Map.of("request", request, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * VM 인스턴스를 시작합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void startInstance(String instanceId) {
        log.info("[VmUseCaseService] startInstance - instanceId={}", instanceId);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.START);
            }
            vmPortRouter.vm(ProviderType.AWS).startInstance(instanceId);

            auditEventPort.record("START_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[VmUseCaseService] startInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[VmUseCaseService] startInstance - failed", e);
            auditEventPort.record("START_INSTANCE", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스를 중지합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void stopInstance(String instanceId) {
        log.info("[VmUseCaseService] stopInstance - instanceId={}", instanceId);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);
            }
            vmPortRouter.vm(ProviderType.AWS).stopInstance(instanceId);

            auditEventPort.record("STOP_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[VmUseCaseService] stopInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[VmUseCaseService] stopInstance - failed", e);
            auditEventPort.record("STOP_INSTANCE", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스를 재부팅합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void rebootInstance(String instanceId) {
        log.info("[VmUseCaseService] rebootInstance - instanceId={}", instanceId);

        try {
            // 재부팅은 STOP/START 조합에 준해 둘 다 지원되는지 확인
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.START);
            }
            vmPortRouter.vm(ProviderType.AWS).rebootInstance(instanceId);

            auditEventPort.record("REBOOT_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[VmUseCaseService] rebootInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[VmUseCaseService] rebootInstance - failed", e);
            auditEventPort.record("REBOOT_INSTANCE", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스를 종료합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void terminateInstance(String instanceId) {
        log.info("[VmUseCaseService] terminateInstance - instanceId={}", instanceId);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);
            }
            vmPortRouter.vm(ProviderType.AWS).terminateInstance(instanceId);

            auditEventPort.record("TERMINATE_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[VmUseCaseService] terminateInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[VmUseCaseService] terminateInstance - failed", e);
            auditEventPort.record("TERMINATE_INSTANCE", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스를 삭제합니다.
     * 
     * @param request 삭제 요청 정보
     */
    @Transactional
    public void deleteInstance(VmDeleteRequest request) {
        log.info("[VmUseCaseService] deleteInstance - request={}", request);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);
            }
            vmPortRouter.vm(ProviderType.AWS).deleteInstance(request);

            auditEventPort.record("DELETE_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", request.getInstanceId(), "request", request));

            log.info("[VmUseCaseService] deleteInstance - success instanceId={}", request.getInstanceId());

        } catch (Exception e) {
            log.error("[VmUseCaseService] deleteInstance - failed", e);
            auditEventPort.record("DELETE_INSTANCE", "VM", "FAILED",
                Map.of("request", request, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * VM 인스턴스 정보를 수정합니다.
     * 
     * @param request 수정 요청 정보
     */
    @Transactional
    public void updateInstance(VmUpdateRequest request) {
        log.info("[VmUseCaseService] updateInstance - request={}", request);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.UPDATE);
            }
            vmPortRouter.vm(ProviderType.AWS).updateInstance(request);

            auditEventPort.record("UPDATE_INSTANCE", "VM", "SUCCESS",
                Map.of("instanceId", request.getInstanceId(), "request", request));

            log.info("[VmUseCaseService] updateInstance - success instanceId={}", request.getInstanceId());

        } catch (Exception e) {
            log.error("[VmUseCaseService] updateInstance - failed", e);
            auditEventPort.record("UPDATE_INSTANCE", "VM", "FAILED",
                Map.of("request", request, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 태그 관리 ====================

    /**
     * VM 인스턴스에 태그를 추가합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     */
    @Transactional
    public void addTags(String instanceId, Map<String, String> tags) {
        log.info("[VmUseCaseService] addTags - instanceId={}, tags={}", instanceId, tags);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);
            }
            vmPortRouter.vm(ProviderType.AWS).addTags(instanceId, tags);

            auditEventPort.record("ADD_TAGS", "VM", "SUCCESS",
                Map.of("instanceId", instanceId, "tags", tags));

            log.info("[VmUseCaseService] addTags - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[VmUseCaseService] addTags - failed", e);
            auditEventPort.record("ADD_TAGS", "VM", "FAILED",
                Map.of("instanceId", instanceId, "tags", tags, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스에서 태그를 제거합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     */
    @Transactional
    public void removeTags(String instanceId, Map<String, String> tagKeys) {
        log.info("[VmUseCaseService] removeTags - instanceId={}, tagKeys={}", instanceId, tagKeys.keySet());

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);
            }
            vmPortRouter.vm(ProviderType.AWS).removeTags(instanceId, tagKeys);

            auditEventPort.record("REMOVE_TAGS", "VM", "SUCCESS",
                Map.of("instanceId", instanceId, "tagKeys", tagKeys.keySet()));

            log.info("[VmUseCaseService] removeTags - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[VmUseCaseService] removeTags - failed", e);
            auditEventPort.record("REMOVE_TAGS", "VM", "FAILED",
                Map.of("instanceId", instanceId, "tagKeys", tagKeys.keySet(), "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 태그 맵
     */
    public Map<String, String> getTags(String instanceId) {
        log.info("[VmUseCaseService] getTags - instanceId={}", instanceId);

        try {
            Map<String, String> tags = vmPortRouter.vm(ProviderType.AWS).getTags(instanceId);

            auditEventPort.record("GET_TAGS", "VM", "SUCCESS",
                Map.of("instanceId", instanceId, "tagCount", tags.size()));

            log.info("[VmUseCaseService] getTags - success instanceId={}, tagCount={}", instanceId, tags.size());
            return tags;

        } catch (Exception e) {
            log.error("[VmUseCaseService] getTags - failed", e);
            auditEventPort.record("GET_TAGS", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 상태 확인 ====================

    /**
     * VM 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    public String getInstanceStatus(String instanceId) {
        log.info("[VmUseCaseService] getInstanceStatus - instanceId={}", instanceId);

        try {
            String status = vmPortRouter.vm(ProviderType.AWS).getInstanceStatus(instanceId);

            auditEventPort.record("GET_INSTANCE_STATUS", "VM", "SUCCESS",
                Map.of("instanceId", instanceId, "status", status));

            log.info("[VmUseCaseService] getInstanceStatus - success instanceId={}, status={}", instanceId, status);
            return status;

        } catch (Exception e) {
            log.error("[VmUseCaseService] getInstanceStatus - failed", e);
            auditEventPort.record("GET_INSTANCE_STATUS", "VM", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    public boolean waitForInstanceStatus(String instanceId, String targetStatus, int timeoutSeconds) {
        log.info("[VmUseCaseService] waitForInstanceStatus - instanceId={}, targetStatus={}, timeout={}s", 
            instanceId, targetStatus, timeoutSeconds);

        try {
            boolean success = vmPortRouter.vm(ProviderType.AWS)
                .waitForInstanceStatus(instanceId, targetStatus, timeoutSeconds);

            auditEventPort.record("WAIT_FOR_INSTANCE_STATUS", "VM", success ? "SUCCESS" : "TIMEOUT",
                Map.of("instanceId", instanceId, "targetStatus", targetStatus, 
                      "timeoutSeconds", timeoutSeconds, "success", success));

            log.info("[VmUseCaseService] waitForInstanceStatus - success={} instanceId={}", success, instanceId);
            return success;

        } catch (Exception e) {
            log.error("[VmUseCaseService] waitForInstanceStatus - failed", e);
            auditEventPort.record("WAIT_FOR_INSTANCE_STATUS", "VM", "FAILED",
                Map.of("instanceId", instanceId, "targetStatus", targetStatus, 
                      "timeoutSeconds", timeoutSeconds, "error", e.getMessage()));
            throw e;
        }
    }
}
