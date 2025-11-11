package com.agenticcp.core.domain.cloud.adapter.outbound;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsAccountSyncAdapter;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.outbound.AccountSyncPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 계정 동기화 포트 라우터
 * 프로바이더 타입에 따라 적절한 AccountSyncPort 구현체로 라우팅합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class AccountSyncPortRouter implements AccountSyncPort {

    private final CloudAccountRepository cloudAccountRepository;
    private final AwsAccountSyncAdapter awsAccountSyncAdapter;
    // 향후 추가될 어댑터들
    // private final AzureAccountSyncAdapter azureAccountSyncAdapter;
    // private final GcpAccountSyncAdapter gcpAccountSyncAdapter;

    /**
     * 프로바이더 타입에 따라 계정 동기화를 수행합니다.
     * 
     * @param accountId 계정 ID
     * @return 동기화된 CloudAccount
     */
    @Override
    public CloudAccount syncAccountInfo(Long accountId) {
        log.debug("[AccountSyncPortRouter] syncAccountInfo - accountId={}", accountId);
        
        // 계정 조회하여 프로바이더 타입 확인
        CloudAccount account = cloudAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        ProviderType providerType = account.getProvider().getProviderType();
        AccountSyncPort adapter = getAdapterForProvider(providerType);
        
        return adapter.syncAccountInfo(accountId);
    }

    /**
     * 프로바이더 타입에 맞는 어댑터를 반환합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return AccountSyncPort 구현체
     * @throws BusinessException 지원하지 않는 프로바이더인 경우
     */
    private AccountSyncPort getAdapterForProvider(ProviderType providerType) {
        switch (providerType) {
            case AWS:
                return awsAccountSyncAdapter;
                
            case AZURE:
                // return azureAccountSyncAdapter;
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    "Azure 프로바이더는 아직 지원되지 않습니다"
                );
                
            case GCP:
                // return gcpAccountSyncAdapter;
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

