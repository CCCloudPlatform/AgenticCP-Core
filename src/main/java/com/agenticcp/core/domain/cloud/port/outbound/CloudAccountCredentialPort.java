package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.CredentialData;

import java.util.Optional;

/**
 * 클라우드 계정 인증 정보 관리 Port
 * 
 * 클라우드 계정의 인증 정보를 안전하게 저장하고 조회하는 Port 인터페이스입니다.
 * 기존 CredentialProviderPort를 확장하여 계정별 인증 정보 관리를 제공합니다.
 * 
 * 실제 구현체는 AWS Secrets Manager, HashiCorp Vault 등의 보안 저장소를 사용하며,
 * 민감 정보는 암호화되어 저장됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public interface CloudAccountCredentialPort {
    
    /**
     * 클라우드 계정의 인증 정보를 암호화하여 저장합니다.
     * 
     * 저장되는 정보는 CSP별로 다릅니다:
     * - AWS: IAM Role ARN, Access Key/Secret Key
     * - GCP: Service Account JSON Key
     * - Azure: Service Principal (Client ID, Secret, Tenant ID)
     * 
     * @param credentialData 저장할 인증 정보
     * @return 저장 성공 여부
     */
    boolean storeCredential(CredentialData credentialData);
    
    /**
     * 클라우드 계정의 인증 정보를 조회하고 복호화합니다.
     * 
     * @param providerType 프로바이더 타입
     * @param tenantKey 테넌트 키
     * @param accountId 계정 ID
     * @return 복호화된 인증 정보 (조회 실패 시 Optional.empty())
     */
    Optional<CredentialData> retrieveCredential(ProviderType providerType, String tenantKey, String accountId);
    
    /**
     * 클라우드 계정의 인증 정보를 업데이트합니다.
     * 
     * credential rotation이나 만료된 인증 정보 갱신 시 사용됩니다.
     * 
     * @param credentialData 업데이트할 인증 정보
     * @return 업데이트 성공 여부
     */
    boolean updateCredential(CredentialData credentialData);
    
    /**
     * 클라우드 계정의 인증 정보를 삭제합니다.
     * 
     * 계정 삭제 시 호출되며, 보안 저장소에서 완전히 제거됩니다.
     * 
     * @param providerType 프로바이더 타입
     * @param tenantKey 테넌트 키
     * @param accountId 계정 ID
     * @return 삭제 성공 여부
     */
    boolean deleteCredential(ProviderType providerType, String tenantKey, String accountId);
    
    /**
     * 클라우드 계정의 인증 정보가 존재하는지 확인합니다.
     * 
     * @param providerType 프로바이더 타입
     * @param tenantKey 테넌트 키
     * @param accountId 계정 ID
     * @return 인증 정보 존재 여부
     */
    boolean existsCredential(ProviderType providerType, String tenantKey, String accountId);
    
    /**
     * 클라우드 계정의 인증 정보가 만료되었는지 확인합니다.
     * 
     * 임시 토큰을 사용하는 경우 만료 시간을 확인합니다.
     * 
     * @param providerType 프로바이더 타입
     * @param tenantKey 테넌트 키
     * @param accountId 계정 ID
     * @return 만료 여부 (true: 만료됨, false: 유효함)
     */
    boolean isCredentialExpired(ProviderType providerType, String tenantKey, String accountId);
    
    /**
     * 기존 CredentialProviderPort와의 호환성을 위한 메서드
     * 
     * 계정 스코프에 해당하는 인증 정보를 Object 형태로 반환합니다.
     * 레거시 코드와의 호환성 유지를 위해 제공됩니다.
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프 (account ID)
     * @return 인증 정보 객체
     */
    Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope);
}

