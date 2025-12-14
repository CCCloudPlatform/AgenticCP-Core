package com.agenticcp.core.domain.cloud.port.outbound.vm;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * VM 조회/탐색 책임 포트
 * 
 * 모든 Discovery 작업에서 세션 자격증명을 전달받아 사용합니다.
 * PR #142의 JIT 세션 관리 패턴을 따릅니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
public interface VmDiscoveryPort {

    /**
     * VM 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건
     * @param session 세션 자격증명
     * @return CloudResource 페이지
     */
    Page<CloudResource> listInstances(VmQuery query, CloudSessionCredential session);

    /**
     * 특정 VM 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    Optional<CloudResource> getInstance(String instanceId, CloudSessionCredential session);

    /**
     * VM 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     * @return 인스턴스 상태
     */
    String getInstanceStatus(String instanceId, CloudSessionCredential session);

    /**
     * VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @param session 세션 자격증명
     * @return 대기 성공 여부
     */
    boolean waitForInstanceStatus(String instanceId, String targetStatus, int timeoutSeconds, CloudSessionCredential session);
}
