package com.agenticcp.core.domain.cloud.port.outbound.rdbms;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

/**
 * RDBMS 생명주기 관리 책임 포트
 * 
 * RDBMS 인스턴스의 시작, 중지, 재시작 기능을 제공합니다.
 * 모든 Lifecycle 작업은 세션 자격증명을 명시적으로 전달받습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface RdbmsLifecyclePort {
    
    /**
     * RDBMS 인스턴스 시작
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     */
    void startInstance(String instanceId, CloudSessionCredential session);
    
    /**
     * RDBMS 인스턴스 중지
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     */
    void stopInstance(String instanceId, CloudSessionCredential session);
    
    /**
     * RDBMS 인스턴스 재시작
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     */
    void rebootInstance(String instanceId, CloudSessionCredential session);
}
