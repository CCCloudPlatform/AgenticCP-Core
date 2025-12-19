package com.agenticcp.core.domain.cloud.port.outbound.function;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionUpdateCommand;

/**
 * Serverless Function 생명주기 관리 포트 인터페이스
 *
 * <p>모든 Management 작업은 세션 자격증명을 명시적으로 전달받습니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface FunctionManagementPort {

    /**
     * Serverless Function 생성
     *
     * @param command 생성 명령
     * @return 생성된 Function CloudResource
     */
    CloudResource createFunction(FunctionCreateCommand command);

    /**
     * Serverless Function 수정
     *
     * @param command 수정 명령
     * @return 수정된 Function CloudResource
     */
    CloudResource updateFunction(FunctionUpdateCommand command);

    /**
     * Serverless Function 삭제
     *
     * @param command 삭제 명령
     */
    void deleteFunction(FunctionDeleteCommand command);
}
