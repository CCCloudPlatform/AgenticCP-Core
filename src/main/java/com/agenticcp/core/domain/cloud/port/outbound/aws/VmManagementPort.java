package com.agenticcp.core.domain.cloud.port.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.Optional;

/**
 * 가상머신(Virtual Machine) 관리 포트 인터페이스
 *
 * VM 인스턴스 생성·조회·수정·삭제 등 라이프사이클 전반을 정의합니다.
 * AWS EC2, Azure VM, GCP Compute Engine 등 다양한 IaaS 제공업체의 구현체가
 * 본 인터페이스를 따르도록 하여 핵사고날 아키텍처의 포트/어댑터 분리를 보장합니다.
 */
public interface VmManagementPort {

    // ==================== 인스턴스 조회 ====================

    /**
     * VM 인스턴스 목록을 조회합니다.
     *
     * @param query 조회 조건 (필터링, 페이징 포함)
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     */
    Page<CloudResource> listInstances(VmQuery query);

    /**
     * 특정 VM 인스턴스를 조회합니다.
     *
     * @param instanceId 인스턴스 ID (null 불가)
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    Optional<CloudResource> getInstance(String instanceId);

    // ==================== 인스턴스 생명주기 ====================

    /**
     * 새로운 VM 인스턴스를 생성합니다.
     *
     * @param request 생성 요청 정보
     * @return 생성된 인스턴스 ID
     */
    String createInstance(VmCreateRequest request);

    /**
     * 중지된 VM 인스턴스를 시작합니다.
     *
     * @param instanceId 인스턴스 ID
     */
    void startInstance(String instanceId);

    /**
     * 실행 중인 VM 인스턴스를 중지합니다.
     *
     * @param instanceId 인스턴스 ID
     */
    void stopInstance(String instanceId);

    /**
     * VM 인스턴스를 재부팅합니다.
     *
     * @param instanceId 인스턴스 ID
     */
    void rebootInstance(String instanceId);

    /**
     * VM 인스턴스를 종료합니다.
     *
     * @param instanceId 인스턴스 ID
     */
    void terminateInstance(String instanceId);

    /**
     * VM 인스턴스를 삭제합니다 (삭제 요청 정보 포함).
     *
     * @param request 삭제 요청 정보 (강제 삭제, 스냅샷 생성 등)
     */
    void deleteInstance(VmDeleteRequest request);

    // ==================== 인스턴스 수정 ====================

    /**
     * VM 인스턴스 정보를 수정합니다.
     *
     * @param request 수정 요청 정보
     */
    void updateInstance(VmUpdateRequest request);

    // ==================== 태그 관리 ====================

    /**
     * VM 인스턴스에 태그를 추가합니다.
     *
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그 (키-값 쌍)
     */
    void addTags(String instanceId, Map<String, String> tags);

    /**
     * VM 인스턴스에서 태그를 제거합니다.
     *
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키 목록
     */
    void removeTags(String instanceId, Map<String, String> tagKeys);

    /**
     * VM 인스턴스의 모든 태그를 조회합니다.
     *
     * @param instanceId 인스턴스 ID
     * @return 태그 맵 (빈 맵 가능, null 반환 금지)
     */
    Map<String, String> getTags(String instanceId);

    // ==================== 상태 확인 ====================

    /**
     * VM 인스턴스의 현재 상태를 확인합니다.
     *
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태 (예: "running", "stopped", "pending")
     */
    String getInstanceStatus(String instanceId);

    /**
     * VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     *
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    boolean waitForInstanceStatus(String instanceId, String targetStatus, int timeoutSeconds);
}
