package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.outbound.AccountSyncPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

/**
 * AWS 계정 동기화 어댑터
 * AWS API를 통해 계정 정보를 최신화합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsAccountSyncAdapter implements AccountSyncPort, ProviderScoped {
    @Override
    public ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }


    private final CloudAccountRepository cloudAccountRepository;
    private final AwsCredentialManager awsCredentialManager;
    private final AwsClientConfig awsClientConfig;

    /**
     * 클라우드 계정 정보를 동기화합니다.
     * AWS STS API를 호출하여 계정이 여전히 유효한지 확인하고,
     * 마지막 동기화 시간을 업데이트합니다.
     * 
     * @param accountId 계정 ID
     * @return 동기화된 CloudAccount
     * @throws BusinessException 동기화 실패 시
     */
    @Override
    public CloudAccount syncAccountInfo(Long accountId) {
        log.info("[AwsAccountSyncAdapter] syncAccountInfo - accountId={}", accountId);
        
        // 계정 조회
        CloudAccount account = cloudAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        StsClient stsClient = null;
        
        try {
            // 자격증명 조회
            AwsCredentialManager.AwsCredentials credentials = 
                awsCredentialManager.getCredentials(account.getCredential().getCredentialKey());
            
            // STS Client 생성
            String region = credentials.getRegion() != null ? credentials.getRegion() : "us-east-1";
            stsClient = awsClientConfig.createStsClient(
                credentials.getAccessKeyId(),
                credentials.getSecretAccessKey(),
                region
            );
            
            // GetCallerIdentity API 호출
            GetCallerIdentityRequest request = GetCallerIdentityRequest.builder().build();
            GetCallerIdentityResponse response = stsClient.getCallerIdentity(request);
            
            // 계정 정보 업데이트
            // AccountScope가 변경되었을 수 있으므로 확인 후 업데이트
            if (response.account() != null && !response.account().equals(account.getAccountScope())) {
                log.warn("[AwsAccountSyncAdapter] syncAccountInfo - accountScope changed: {} -> {}", 
                         account.getAccountScope(), response.account());
                account.setAccountScope(response.account());
            }
            
            // 동기화 시간 업데이트
            account.updateLastSyncTime();
            
            // 계정이 활성 상태가 아니면 활성화
            if (!account.isActive()) {
                account.markAsVerified();
                log.info("[AwsAccountSyncAdapter] syncAccountInfo - account reactivated");
            }
            
            CloudAccount synced = cloudAccountRepository.save(account);
            
            log.info("[AwsAccountSyncAdapter] syncAccountInfo - success");
            return synced;
            
        } catch (BusinessException e) {
            // 자격증명 조회 실패 등의 비즈니스 예외는 그대로 전파
            throw e;
            
        } catch (Exception e) {
            log.error("[AwsAccountSyncAdapter] syncAccountInfo - sync failed", e);
            
            // 동기화 실패 시에도 lastSyncAt는 업데이트 (실패 기록)
            account.updateLastSyncTime();
            account.markAsFailed();
            cloudAccountRepository.save(account);
            
            throw new BusinessException(
                CloudErrorCode.PROVIDER_UNAVAILABLE,
                "AWS 계정 동기화 실패: " + e.getMessage()
            );
            
        } finally {
            // STS Client 종료
            if (stsClient != null) {
                try {
                    stsClient.close();
                } catch (Exception e) {
                    log.warn("[AwsAccountSyncAdapter] Failed to close STS client", e);
                }
            }
        }
    }
}

