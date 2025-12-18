package com.agenticcp.core.domain.cloud.port.outbound.rdbms;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsQuery;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * RDBMS 조회/탐색 책임 포트
 * 
 * RDBMS 인스턴스의 조회 기능을 제공합니다.
 * 모든 Discovery 작업에서 세션 자격증명을 전달받아 사용합니다.
 * JIT 세션 관리 패턴을 따릅니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface RdbmsDiscoveryPort {
    
    /**
     * RDBMS 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건 (페이징, 필터링 포함)
     * @param session 세션 자격증명
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     */
    Page<CloudResource> listRdbmsInstances(RdbmsQuery query, CloudSessionCredential session);
    
    /**
     * 특정 RDBMS 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    Optional<CloudResource> getRdbmsInstance(String instanceId, CloudSessionCredential session);
    
    /**
     * RDBMS 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     * @return 인스턴스 상태
     */
    String getInstanceStatus(String instanceId, CloudSessionCredential session);
}
