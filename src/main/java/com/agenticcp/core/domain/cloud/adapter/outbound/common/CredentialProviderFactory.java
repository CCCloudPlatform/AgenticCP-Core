package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.AwsErrorCode;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsCredentialProviderPortAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

/**
 * 자격증명 제공자 팩토리
 * 
 * 프로바이더별 자격증명 어댑터를 선택하여 반환하는 팩토리 클래스입니다.
 * 헥사고날 아키텍처에서 프로바이더별 어댑터 분리를 지원합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CredentialProviderFactory {
    
    private final AwsCredentialProviderPortAdapter awsAdapter;
    // TODO: Azure, GCP 어댑터 추가 예정
    // private final AzureCredentialProviderPortAdapter azureAdapter;
    // private final GcpCredentialProviderPortAdapter gcpAdapter;
    
    /**
     * 프로바이더 타입에 따른 자격증명 어댑터 반환
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return 해당 프로바이더의 자격증명 어댑터
     * @throws BusinessException 지원하지 않는 프로바이더 타입인 경우
     */
    public CredentialProviderPort getAdapter(CloudProvider.ProviderType providerType) {
        log.debug("자격증명 어댑터 선택: providerType={}", providerType);
        
        return switch (providerType) {
            case AWS -> {
                log.debug("AWS 자격증명 어댑터 선택");
                yield awsAdapter;
            }
            case AZURE -> {
                log.warn("Azure 자격증명 어댑터는 아직 구현되지 않았습니다");
                throw new BusinessException(AwsErrorCode.AWS_CONFIGURATION_ERROR, 
                        "Azure 자격증명 해결은 아직 구현되지 않았습니다.");
            }
            case GCP -> {
                log.warn("GCP 자격증명 어댑터는 아직 구현되지 않았습니다");
                throw new BusinessException(AwsErrorCode.AWS_CONFIGURATION_ERROR, 
                        "GCP 자격증명 해결은 아직 구현되지 않았습니다.");
            }
            default -> {
                log.error("지원하지 않는 프로바이더 타입: {}", providerType);
                throw new BusinessException(AwsErrorCode.AWS_CONFIGURATION_ERROR, 
                        "지원하지 않는 프로바이더 타입: " + providerType);
            }
        };
    }
    
    /**
     * 자격증명 해결 (편의 메서드)
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @return AWS 자격증명
     */
    public AwsCredentials resolveCredentials(String tenantKey, CloudProvider.ProviderType providerType, String accountScope) {
        CredentialProviderPort adapter = getAdapter(providerType);
        return (AwsCredentials) adapter.resolveCredentials(tenantKey, providerType, accountScope);
    }
}
