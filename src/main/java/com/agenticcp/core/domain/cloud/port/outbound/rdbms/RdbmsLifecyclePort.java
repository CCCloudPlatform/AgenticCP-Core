package com.agenticcp.core.domain.cloud.port.outbound.rdbms;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceLifecyclePort;

/**
 * RDBMS 생명주기 관리 책임 포트
 * 
 * RDBMS 인스턴스의 시작, 중지, 재시작 기능을 제공합니다.
 * ResourceLifecyclePort를 확장하여 일반적인 리소스 생명주기 관리 기능을 상속받고,
 * RDBMS 특화 기능인 reboot를 추가로 제공합니다.
 * 
 * 모든 Lifecycle 작업은 세션 자격증명을 명시적으로 전달받습니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
public interface RdbmsLifecyclePort extends ResourceLifecyclePort {
    
    /**
     * RDBMS 인스턴스 재시작
     * 
     * <p>일반적인 리소스의 경우 stop 후 start를 순차적으로 호출하지만,
     * RDBMS의 경우 reboot는 OS 레벨 재부팅으로 더 빠르고 안전한 재시작을 제공합니다.</p>
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     */
    void rebootInstance(String instanceId, CloudSessionCredential session);
}
