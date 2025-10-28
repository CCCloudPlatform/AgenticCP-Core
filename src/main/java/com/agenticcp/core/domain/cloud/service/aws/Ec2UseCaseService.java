package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import org.springframework.beans.factory.annotation.Autowired;
import com.agenticcp.core.domain.cloud.port.model.Ec2CreateRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2DeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2Query;
import com.agenticcp.core.domain.cloud.port.model.Ec2UpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * EC2 유스케이스 서비스
 * 
 * EC2 인스턴스 관리에 대한 비즈니스 로직을 담당하는 서비스입니다.
 * 핵사고날 아키텍처의 애플리케이션 계층에 해당하며, 포트를 통해 외부 시스템과 통신합니다.
 * 
 * 주요 기능:
 * - EC2 인스턴스 조회, 생성, 수정, 삭제
 * - 인스턴스 생명주기 관리 (시작, 중지, 재부팅, 종료)
 * - 태그 관리 및 상태 확인
 * - 감사 로그 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Ec2UseCaseService {

    private final Ec2PortRouter ec2PortRouter;
    private final AuditEventPort auditEventPort;
    @Autowired(required = false)
    private CapabilityGuard capabilityGuard;

    private static final String SERVICE_KEY = "EC2";
    private static final String RESOURCE_TYPE = "INSTANCE";

    // ==================== 인스턴스 조회 ====================

    /**
     * EC2 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건
     * @return CloudResource 페이지
     */
    public Page<CloudResource> listInstances(Ec2Query query) {
        log.info("[Ec2UseCaseService] listInstances - query={}", query);

        try {
            Page<CloudResource> result = ec2PortRouter.ec2(ProviderType.AWS)
                .listInstances(query);

            auditEventPort.record("LIST_INSTANCES", "EC2", "SUCCESS",
                Map.of("count", result.getTotalElements(), "query", query));

            log.info("[Ec2UseCaseService] listInstances - success count={}", result.getTotalElements());
            return result;

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] listInstances - failed", e);
            auditEventPort.record("LIST_INSTANCES", "EC2", "FAILED",
                Map.of("error", e.getMessage(), "query", query));
            throw e;
        }
    }

    /**
     * 특정 EC2 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    public Optional<CloudResource> getInstance(String instanceId) {
        log.info("[Ec2UseCaseService] getInstance - instanceId={}", instanceId);

        try {
            Optional<CloudResource> result = ec2PortRouter.ec2(ProviderType.AWS)
                .getInstance(instanceId);

            auditEventPort.record("GET_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId, "found", result.isPresent()));

            log.info("[Ec2UseCaseService] getInstance - success found={}", result.isPresent());
            return result;

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] getInstance - failed", e);
            auditEventPort.record("GET_INSTANCE", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 EC2 인스턴스를 생성합니다.
     * 
     * @param request 생성 요청 정보
     * @return 생성된 인스턴스 ID
     */
    @Transactional
    public String createInstance(Ec2CreateRequest request) {
        log.info("[Ec2UseCaseService] createInstance - request={}", request);

        try {
            String instanceId = ec2PortRouter.ec2(ProviderType.AWS)
                .createInstance(request);

            auditEventPort.record("CREATE_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId, "request", request));

            log.info("[Ec2UseCaseService] createInstance - success instanceId={}", instanceId);
            return instanceId;

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] createInstance - failed", e);
            auditEventPort.record("CREATE_INSTANCE", "EC2", "FAILED",
                Map.of("request", request, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * EC2 인스턴스를 시작합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void startInstance(String instanceId) {
        log.info("[Ec2UseCaseService] startInstance - instanceId={}", instanceId);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.START);
            }
            ec2PortRouter.ec2(ProviderType.AWS).startInstance(instanceId);

            auditEventPort.record("START_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[Ec2UseCaseService] startInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] startInstance - failed", e);
            auditEventPort.record("START_INSTANCE", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 중지합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void stopInstance(String instanceId) {
        log.info("[Ec2UseCaseService] stopInstance - instanceId={}", instanceId);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);
            }
            ec2PortRouter.ec2(ProviderType.AWS).stopInstance(instanceId);

            auditEventPort.record("STOP_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[Ec2UseCaseService] stopInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] stopInstance - failed", e);
            auditEventPort.record("STOP_INSTANCE", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 재부팅합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void rebootInstance(String instanceId) {
        log.info("[Ec2UseCaseService] rebootInstance - instanceId={}", instanceId);

        try {
            // 재부팅은 STOP/START 조합에 준해 둘 다 지원되는지 확인
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.STOP);
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.START);
            }
            ec2PortRouter.ec2(ProviderType.AWS).rebootInstance(instanceId);

            auditEventPort.record("REBOOT_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[Ec2UseCaseService] rebootInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] rebootInstance - failed", e);
            auditEventPort.record("REBOOT_INSTANCE", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 종료합니다.
     * 
     * @param instanceId 인스턴스 ID
     */
    @Transactional
    public void terminateInstance(String instanceId) {
        log.info("[Ec2UseCaseService] terminateInstance - instanceId={}", instanceId);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);
            }
            ec2PortRouter.ec2(ProviderType.AWS).terminateInstance(instanceId);

            auditEventPort.record("TERMINATE_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId));

            log.info("[Ec2UseCaseService] terminateInstance - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] terminateInstance - failed", e);
            auditEventPort.record("TERMINATE_INSTANCE", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 삭제합니다.
     * 
     * @param request 삭제 요청 정보
     */
    @Transactional
    public void deleteInstance(Ec2DeleteRequest request) {
        log.info("[Ec2UseCaseService] deleteInstance - request={}", request);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);
            }
            ec2PortRouter.ec2(ProviderType.AWS).deleteInstance(request);

            auditEventPort.record("DELETE_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", request.getInstanceId(), "request", request));

            log.info("[Ec2UseCaseService] deleteInstance - success instanceId={}", request.getInstanceId());

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] deleteInstance - failed", e);
            auditEventPort.record("DELETE_INSTANCE", "EC2", "FAILED",
                Map.of("request", request, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * EC2 인스턴스 정보를 수정합니다.
     * 
     * @param request 수정 요청 정보
     */
    @Transactional
    public void updateInstance(Ec2UpdateRequest request) {
        log.info("[Ec2UseCaseService] updateInstance - request={}", request);

        try {
            ec2PortRouter.ec2(ProviderType.AWS).updateInstance(request);

            auditEventPort.record("UPDATE_INSTANCE", "EC2", "SUCCESS",
                Map.of("instanceId", request.getInstanceId(), "request", request));

            log.info("[Ec2UseCaseService] updateInstance - success instanceId={}", request.getInstanceId());

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] updateInstance - failed", e);
            auditEventPort.record("UPDATE_INSTANCE", "EC2", "FAILED",
                Map.of("request", request, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 태그 관리 ====================

    /**
     * EC2 인스턴스에 태그를 추가합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     */
    @Transactional
    public void addTags(String instanceId, Map<String, String> tags) {
        log.info("[Ec2UseCaseService] addTags - instanceId={}, tags={}", instanceId, tags);

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);
            }
            ec2PortRouter.ec2(ProviderType.AWS).addTags(instanceId, tags);

            auditEventPort.record("ADD_TAGS", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId, "tags", tags));

            log.info("[Ec2UseCaseService] addTags - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] addTags - failed", e);
            auditEventPort.record("ADD_TAGS", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "tags", tags, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스에서 태그를 제거합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     */
    @Transactional
    public void removeTags(String instanceId, Map<String, String> tagKeys) {
        log.info("[Ec2UseCaseService] removeTags - instanceId={}, tagKeys={}", instanceId, tagKeys.keySet());

        try {
            if (capabilityGuard != null) {
                capabilityGuard.ensureSupported(ProviderType.AWS, SERVICE_KEY, RESOURCE_TYPE, CapabilityGuard.Operation.TAGGING);
            }
            ec2PortRouter.ec2(ProviderType.AWS).removeTags(instanceId, tagKeys);

            auditEventPort.record("REMOVE_TAGS", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId, "tagKeys", tagKeys.keySet()));

            log.info("[Ec2UseCaseService] removeTags - success instanceId={}", instanceId);

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] removeTags - failed", e);
            auditEventPort.record("REMOVE_TAGS", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "tagKeys", tagKeys.keySet(), "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 태그 맵
     */
    public Map<String, String> getTags(String instanceId) {
        log.info("[Ec2UseCaseService] getTags - instanceId={}", instanceId);

        try {
            Map<String, String> tags = ec2PortRouter.ec2(ProviderType.AWS).getTags(instanceId);

            auditEventPort.record("GET_TAGS", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId, "tagCount", tags.size()));

            log.info("[Ec2UseCaseService] getTags - success instanceId={}, tagCount={}", instanceId, tags.size());
            return tags;

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] getTags - failed", e);
            auditEventPort.record("GET_TAGS", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    // ==================== 상태 확인 ====================

    /**
     * EC2 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    public String getInstanceStatus(String instanceId) {
        log.info("[Ec2UseCaseService] getInstanceStatus - instanceId={}", instanceId);

        try {
            String status = ec2PortRouter.ec2(ProviderType.AWS).getInstanceStatus(instanceId);

            auditEventPort.record("GET_INSTANCE_STATUS", "EC2", "SUCCESS",
                Map.of("instanceId", instanceId, "status", status));

            log.info("[Ec2UseCaseService] getInstanceStatus - success instanceId={}, status={}", instanceId, status);
            return status;

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] getInstanceStatus - failed", e);
            auditEventPort.record("GET_INSTANCE_STATUS", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "error", e.getMessage()));
            throw e;
        }
    }

    /**
     * EC2 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    public boolean waitForInstanceStatus(String instanceId, String targetStatus, int timeoutSeconds) {
        log.info("[Ec2UseCaseService] waitForInstanceStatus - instanceId={}, targetStatus={}, timeout={}s", 
            instanceId, targetStatus, timeoutSeconds);

        try {
            boolean success = ec2PortRouter.ec2(ProviderType.AWS)
                .waitForInstanceStatus(instanceId, targetStatus, timeoutSeconds);

            auditEventPort.record("WAIT_FOR_INSTANCE_STATUS", "EC2", success ? "SUCCESS" : "TIMEOUT",
                Map.of("instanceId", instanceId, "targetStatus", targetStatus, 
                      "timeoutSeconds", timeoutSeconds, "success", success));

            log.info("[Ec2UseCaseService] waitForInstanceStatus - success={} instanceId={}", success, instanceId);
            return success;

        } catch (Exception e) {
            log.error("[Ec2UseCaseService] waitForInstanceStatus - failed", e);
            auditEventPort.record("WAIT_FOR_INSTANCE_STATUS", "EC2", "FAILED",
                Map.of("instanceId", instanceId, "targetStatus", targetStatus, 
                      "timeoutSeconds", timeoutSeconds, "error", e.getMessage()));
            throw e;
        }
    }
}
