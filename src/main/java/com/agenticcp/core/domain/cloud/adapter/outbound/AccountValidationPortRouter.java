package com.agenticcp.core.domain.cloud.adapter.outbound;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsAccountValidationAdapter;
import com.agenticcp.core.domain.cloud.dto.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.dto.AccountValidationResult;
import com.agenticcp.core.domain.cloud.dto.ConnectionTestResult;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.outbound.AccountValidationPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 계정 검증 포트 라우터
 * 프로바이더 타입에 따라 적절한 AccountValidationPort 구현체로 라우팅합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class AccountValidationPortRouter implements AccountValidationPort {

    private final CloudAccountRepository cloudAccountRepository;
    private final AwsAccountValidationAdapter awsAccountValidationAdapter;
    // 향후 추가될 어댑터들
    // private final AzureAccountValidationAdapter azureAccountValidationAdapter;
    // private final GcpAccountValidationAdapter gcpAccountValidationAdapter;

    /**
     * 프로바이더 타입에 따라 계정 검증을 수행합니다.
     * 
     * @param request 검증 요청
     * @return AccountValidationResult 검증 결과
     */
    @Override
    public AccountValidationResult validateAccount(AccountValidationRequest request) {
        log.debug("[AccountValidationPortRouter] validateAccount - providerType={}", 
                  request.getProviderType());
        
        AccountValidationPort adapter = getAdapterForProvider(request.getProviderType());
        return adapter.validateAccount(request);
    }

    /**
     * 프로바이더 타입에 따라 연결 테스트를 수행합니다.
     * 
     * @param accountId 계정 ID
     * @param credentials 자격증명 정보
     * @return ConnectionTestResult 연결 테스트 결과
     */
    @Override
    public ConnectionTestResult testConnection(Long accountId, Map<String, String> credentials) {
        log.debug("[AccountValidationPortRouter] testConnection - accountId={}", accountId);
        
        // 계정 조회하여 프로바이더 타입 확인
        CloudAccount account = cloudAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        ProviderType providerType = account.getProvider().getProviderType();
        AccountValidationPort adapter = getAdapterForProvider(providerType);
        
        return adapter.testConnection(accountId, credentials);
    }

    /**
     * 프로바이더 타입에 맞는 어댑터를 반환합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return AccountValidationPort 구현체
     * @throws BusinessException 지원하지 않는 프로바이더인 경우
     */
    private AccountValidationPort getAdapterForProvider(ProviderType providerType) {
        switch (providerType) {
            case AWS:
                return awsAccountValidationAdapter;
                
            case AZURE:
                // return azureAccountValidationAdapter;
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    "Azure 프로바이더는 아직 지원되지 않습니다"
                );
                
            case GCP:
                // return gcpAccountValidationAdapter;
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    "GCP 프로바이더는 아직 지원되지 않습니다"
                );
                
            default:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    "지원하지 않는 프로바이더입니다: " + providerType
                );
        }
    }
}

