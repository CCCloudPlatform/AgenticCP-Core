package com.agenticcp.core.domain.cloud.port.outbound.account;

import com.agenticcp.core.domain.cloud.port.model.account.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.account.ConnectionTestResult;
import com.agenticcp.core.domain.cloud.port.model.account.AccountValidationResult;

import java.util.Map;

/**
 * 클라우드 계정 검증을 위한 포트
 * 프로바이더별 실제 API 호출을 통해 계정 유효성을 검증합니다.
 * 
 * 이 포트는 헥사고날 아키텍처의 아웃바운드 포트로,
 * 도메인 계층이 외부 클라우드 프로바이더와 통신하기 위한 인터페이스입니다.
 * 
 * 검증 성공 시 AccountId 등의 메타데이터도 함께 반환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface AccountValidationPort {
    
    /**
     * 계정 자격증명의 유효성을 검증합니다.
     * 
     * 실제 클라우드 프로바이더 API를 호출하여 자격증명이 유효한지 확인하고,
     * 검증 성공 시 AccountId, Region 등의 정보를 반환합니다.
     * 
     * AWS의 경우 STS GetCallerIdentity API를 호출하여 검증합니다.
     * 
     * @param request 검증 요청 정보 (자격증명, 프로바이더 타입 등)
     * @return ValidationResult 검증 결과 (valid, accountId, region, metadata 포함)
     * @throws com.agenticcp.core.common.exception.BusinessException 검증 실패 시 
     *         (잘못된 자격증명, 네트워크 오류 등)
     */
    AccountValidationResult validateAccount(AccountValidationRequest request);
    
    /**
     * 등록된 계정의 연결을 테스트합니다.
     * 
     * 이미 등록된 계정의 자격증명을 사용하여 
     * 현재 클라우드 프로바이더와의 연결 상태를 확인합니다.
     * 
     * @param accountId 테스트할 계정 ID
     * @param credentials 자격증명 정보 (accessKeyId, secretAccessKey 등)
     * @return ConnectionTestResult 연결 테스트 결과
     * @throws com.agenticcp.core.common.exception.BusinessException 연결 테스트 실패 시
     */
    ConnectionTestResult testConnection(Long accountId, Map<String, String> credentials);
}

