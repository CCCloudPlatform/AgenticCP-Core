package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;

/**
 * 리소스 생명주기 관리 포트
 * 
 * 리소스의 시작, 중지, 종료 등의 생명주기 작업을 관리합니다.
 * 세션 자격증명을 받아서 사용합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface ResourceLifecyclePort {
    
    /**
     * 리소스를 시작합니다.
     * 
     * @param id 리소스 식별자
     * @param session 세션 자격증명
     */
    void start(ResourceIdentity id, CloudSessionCredential session);
    
    /**
     * 리소스를 중지합니다.
     * 
     * @param id 리소스 식별자
     * @param session 세션 자격증명
     */
    void stop(ResourceIdentity id, CloudSessionCredential session);
    
    /**
     * 리소스를 종료합니다.
     * 
     * @param id 리소스 식별자
     * @param session 세션 자격증명
     */
    void terminate(ResourceIdentity id, CloudSessionCredential session);
}
