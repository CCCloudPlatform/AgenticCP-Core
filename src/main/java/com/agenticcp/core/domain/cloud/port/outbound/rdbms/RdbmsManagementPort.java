package com.agenticcp.core.domain.cloud.port.outbound.rdbms;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsUpdateCommand;

/**
 * RDBMS 관리 포트 - RDBMS 인스턴스의 CRUD 작업을 정의하는 계약
 * 
 * RDBMS 인스턴스의 생성, 수정, 삭제 기능을 제공합니다.
 * 모든 Management 작업은 세션 자격증명을 명시적으로 전달받습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface RdbmsManagementPort {
    
    /**
     * RDBMS 인스턴스 생성
     * 
     * @param command 생성 명령
     * @return 생성된 RDBMS 인스턴스
     */
    CloudResource createRdbms(RdbmsCreateCommand command);
    
    /**
     * RDBMS 인스턴스 수정
     * 
     * @param command 수정 명령
     * @return 수정된 RDBMS 인스턴스
     */
    CloudResource updateRdbms(RdbmsUpdateCommand command);
    
    /**
     * RDBMS 인스턴스 삭제
     * 
     * @param command 삭제 명령
     */
    void deleteRdbms(RdbmsDeleteCommand command);
}
