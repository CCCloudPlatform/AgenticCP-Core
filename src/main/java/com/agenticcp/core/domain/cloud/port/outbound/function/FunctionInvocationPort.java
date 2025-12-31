package com.agenticcp.core.domain.cloud.port.outbound.function;

import com.agenticcp.core.domain.cloud.port.model.function.FunctionInvokeCommand;

/**
 * Serverless Function 실행 포트 인터페이스
 *
 * <p>Function 실행 작업은 세션 자격증명을 명시적으로 전달받습니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface FunctionInvocationPort {

    /**
     * Serverless Function 실행 (동기)
     *
     * @param command 실행 커맨드
     * @return 실행 결과 (JSON 문자열)
     */
    String invokeFunction(FunctionInvokeCommand command);

    /**
     * Serverless Function 비동기 실행
     *
     * @param command 실행 커맨드
     * @return 요청 ID (추적용)
     */
    String invokeFunctionAsync(FunctionInvokeCommand command);
}
