package com.agenticcp.core.domain.cloud.adapter.router;

import com.agenticcp.core.domain.cloud.adapter.aws.AwsAccountValidationAdapter;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.AccountMetadata;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationResult;
import com.agenticcp.core.domain.cloud.port.outbound.CloudAccountValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 클라우드 계정 검증 어댑터 라우터
 * 
 * Strategy Pattern을 사용하여 프로바이더별 어댑터를 선택하고 라우팅합니다.
 * 현재는 AWS만 구현되어 있으며, 향후 GCP, Azure 어댑터가 추가될 예정입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudAccountValidationAdapterRouter implements CloudAccountValidationPort {
    
    private final AwsAccountValidationAdapter awsAdapter;
    
    // 향후 추가될 어댑터들
    // private final GcpAccountValidationAdapter gcpAdapter;
    // private final AzureAccountValidationAdapter azureAdapter;
    
    @Override
    public AccountValidationResult validateAccount(AccountValidationRequest request) {
        log.info("[CloudAccountValidationAdapterRouter] validateAccount - providerType={}, accountId={}", 
                request.getProviderType(), request.getAccountId());
        
        CloudAccountValidationPort adapter = getAdapter(request.getProviderType());
        
        if (adapter == null) {
            log.error("[CloudAccountValidationAdapterRouter] validateAccount - unsupported provider: {}", 
                    request.getProviderType());
            
            return AccountValidationResult.failure(
                    request.getAccountId(),
                    request.getProviderType(),
                    "UNSUPPORTED_PROVIDER",
                    "Unsupported cloud provider: " + request.getProviderType()
            );
        }
        
        return adapter.validateAccount(request);
    }
    
    @Override
    public AccountMetadata getAccountMetadata(AccountValidationRequest request) {
        log.info("[CloudAccountValidationAdapterRouter] getAccountMetadata - providerType={}, accountId={}", 
                request.getProviderType(), request.getAccountId());
        
        CloudAccountValidationPort adapter = getAdapter(request.getProviderType());
        
        if (adapter == null) {
            log.error("[CloudAccountValidationAdapterRouter] getAccountMetadata - unsupported provider: {}", 
                    request.getProviderType());
            return null;
        }
        
        return adapter.getAccountMetadata(request);
    }
    
    @Override
    public AccountValidationResult testConnection(AccountValidationRequest request) {
        log.info("[CloudAccountValidationAdapterRouter] testConnection - providerType={}, accountId={}", 
                request.getProviderType(), request.getAccountId());
        
        CloudAccountValidationPort adapter = getAdapter(request.getProviderType());
        
        if (adapter == null) {
            log.error("[CloudAccountValidationAdapterRouter] testConnection - unsupported provider: {}", 
                    request.getProviderType());
            
            return AccountValidationResult.failure(
                    request.getAccountId(),
                    request.getProviderType(),
                    "UNSUPPORTED_PROVIDER",
                    "Unsupported cloud provider: " + request.getProviderType()
            );
        }
        
        return adapter.testConnection(request);
    }
    
    /**
     * 프로바이더 타입에 따라 적절한 어댑터를 반환합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return 해당 프로바이더의 어댑터 또는 null (지원하지 않는 프로바이더)
     */
    private CloudAccountValidationPort getAdapter(ProviderType providerType) {
        if (providerType == null) {
            log.error("[CloudAccountValidationAdapterRouter] Provider type is null");
            return null;
        }
        
        switch (providerType) {
            case AWS:
                return awsAdapter;
            case GCP:
                // TODO: GCP 어댑터 구현 예정
                log.warn("[CloudAccountValidationAdapterRouter] GCP adapter not yet implemented");
                return null;
            case AZURE:
                // TODO: Azure 어댑터 구현 예정
                log.warn("[CloudAccountValidationAdapterRouter] Azure adapter not yet implemented");
                return null;
            default:
                log.error("[CloudAccountValidationAdapterRouter] Unknown provider type: {}", providerType);
                return null;
        }
    }
}
