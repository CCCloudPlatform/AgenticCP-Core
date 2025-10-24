package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.port.model.AccountMetadata;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationResult;

/**
 * 클라우드 계정 검증 Port
 * 
 * 외부 클라우드 API를 통해 계정 유효성을 검증하는 Port 인터페이스입니다.
 * 헥사고날 아키텍처의 Outbound Port로, 실제 구현은 Adapter에서 수행됩니다.
 * 
 * 각 CSP(AWS, GCP, Azure)별 Adapter가 이 인터페이스를 구현하며,
 * Router가 ProviderType에 따라 적절한 Adapter로 요청을 라우팅합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public interface CloudAccountValidationPort {
    
    /**
     * 클라우드 계정 유효성을 검증합니다.
     * 
     * 각 CSP별 검증 내용:
     * - AWS: STS AssumeRole, IAM 권한 확인
     * - GCP: Service Account 권한 확인, Project 접근성 테스트
     * - Azure: Service Principal 권한 확인, Subscription 접근성 테스트
     * 
     * @param request 계정 검증 요청 정보
     * @return 검증 결과 (성공/실패, 에러 메시지 포함)
     */
    AccountValidationResult validateAccount(AccountValidationRequest request);
    
    /**
     * 클라우드 계정의 메타데이터를 조회합니다.
     * 
     * 조회되는 정보:
     * - 계정명, 소유자 정보
     * - 사용 가능한 리전/존/로케이션 목록
     * - 활성화된 서비스 목록
     * - CSP별 추가 메타데이터
     * 
     * @param request 계정 메타데이터 조회 요청 정보
     * @return 계정 메타데이터 (조회 실패 시 null 또는 빈 정보)
     */
    AccountMetadata getAccountMetadata(AccountValidationRequest request);
    
    /**
     * 클라우드 계정의 연결 상태를 실시간으로 테스트합니다.
     * 
     * validateAccount()보다 가벼운 검증으로, 빠른 연결 확인을 위해 사용됩니다.
     * 실제 API 호출을 통해 현재 인증 정보가 유효한지만 확인합니다.
     * 
     * @param request 연결 테스트 요청 정보
     * @return 연결 테스트 결과
     */
    AccountValidationResult testConnection(AccountValidationRequest request);
}

