package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;

import java.util.Map;

/**
 * 클라우드 자격증명 제공을 위한 포트
 * 
 * 자격증명의 저장, 조회, 삭제 등의 기능을 제공합니다.
 * 이 포트는 헥사고날 아키텍처의 아웃바운드 포트로,
 * 도메인 계층이 자격증명 관리 시스템과 통신하기 위한 인터페이스입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface CredentialProviderPort {
    
    /**
     * 자격증명을 해결하고 반환합니다.
     * 
     * 저장된 암호화된 자격증명을 복호화하여 실제 사용 가능한 형태로 반환합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 범위 (AccountId, SubscriptionId, ProjectId)
     * @return 자격증명 객체 (프로바이더별 타입)
     * @throws com.agenticcp.core.common.exception.BusinessException 자격증명 조회 실패 시
     */
    Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope);
    
    /**
     * 자격증명을 저장합니다 (암호화 포함).
     * 
     * 평문 자격증명을 암호화하여 안전하게 저장합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 범위
     * @param credentials 자격증명 정보 (평문)
     * @return 저장된 자격증명의 키 참조 (UUID)
     * @throws com.agenticcp.core.common.exception.BusinessException 자격증명 저장 실패 시
     */
    String storeCredentials(String tenantKey, ProviderType providerType, 
                           String accountScope, Map<String, String> credentials);
    
    /**
     * 자격증명을 삭제합니다.
     * 
     * @param credentialKey 자격증명 키 참조 (UUID)
     * @throws com.agenticcp.core.common.exception.BusinessException 자격증명 삭제 실패 시
     */
    void deleteCredentials(String credentialKey);
}
