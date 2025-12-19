package com.agenticcp.core.domain.cloud.port.outbound.function;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionQuery;
import com.agenticcp.core.domain.cloud.port.model.function.GetFunctionCommand;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Serverless Function 조회/탐색 포트 인터페이스
 *
 * <p>모든 Discovery 작업에서 세션 자격증명을 전달받아 사용합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface FunctionDiscoveryPort {

    /**
     * Serverless Function 목록 조회
     *
     * @param query 조회 조건 (페이징, 필터링 포함)
     * @param session 세션 자격증명
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     */
    Page<CloudResource> listFunctions(FunctionQuery query, CloudSessionCredential session);

    /**
     * 특정 Serverless Function 조회
     *
     * @param command 조회 명령
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    Optional<CloudResource> getFunction(GetFunctionCommand command);

    /**
     * Serverless Function 상태 조회
     *
     * @param functionId 함수 ID (providerResourceId)
     * @param session 세션 자격증명
     * @return 함수 상태 문자열
     */
    String getFunctionStatus(String functionId, CloudSessionCredential session);

    /**
     * Serverless Function 코드 조회
     *
     * @param functionId 함수 ID (providerResourceId)
     * @param session 세션 자격증명
     * @return 함수 코드 (ZIP 바이너리)
     */
    byte[] getFunctionCode(String functionId, CloudSessionCredential session);
}
