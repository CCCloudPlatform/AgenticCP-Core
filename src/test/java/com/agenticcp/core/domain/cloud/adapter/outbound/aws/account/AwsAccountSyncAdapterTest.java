package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsAccountSyncAdapter 테스트")
class AwsAccountSyncAdapterTest {

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @Mock
    private AwsCredentialManager awsCredentialManager;

    @Mock
    private AwsClientConfig awsClientConfig;

    @Mock
    private StsClient stsClient;

    @InjectMocks
    private AwsAccountSyncAdapter adapter;

    @Nested
    @DisplayName("syncAccountInfo")
    class SyncAccountInfoTest {

        @Test
        @DisplayName("정상적으로 계정을 동기화하고 상태를 업데이트한다")
        void syncAccountInfo_Success() {
            // given
            Long accountId = 1L;
            CloudAccount account = buildAccount("old-account", AccountStatus.INACTIVE);

            when(cloudAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(awsCredentialManager.getCredentials("cred-123")).thenReturn(
                    AwsCredentialManager.AwsCredentials.builder()
                            .accessKeyId("access")
                            .secretAccessKey("secret")
                            .region("ap-northeast-2")
                            .build()
            );
            when(awsClientConfig.createStsClient("access", "secret", "ap-northeast-2")).thenReturn(stsClient);
            when(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                    .thenReturn(GetCallerIdentityResponse.builder().account("999999999999").build());
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            CloudAccount result = adapter.syncAccountInfo(accountId);

            // then
            assertThat(result.getAccountScope()).isEqualTo("999999999999");
            assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.VERIFIED);
            assertThat(result.getLastSyncAt()).isNotNull();

            verify(awsCredentialManager).getCredentials("cred-123");
            verify(awsClientConfig).createStsClient("access", "secret", "ap-northeast-2");
            verify(stsClient).getCallerIdentity(any(GetCallerIdentityRequest.class));

            ArgumentCaptor<CloudAccount> savedCaptor = ArgumentCaptor.forClass(CloudAccount.class);
            verify(cloudAccountRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getAccountScope()).isEqualTo("999999999999");

            verify(stsClient).close();
        }

        @Test
        @DisplayName("계정을 찾을 수 없으면 예외가 발생한다")
        void syncAccountInfo_AccountNotFound() {
            // given
            Long accountId = 10L;
            when(cloudAccountRepository.findById(accountId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adapter.syncAccountInfo(accountId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CloudErrorCode.ACCOUNT_NOT_FOUND));

            verifyNoInteractions(awsCredentialManager, awsClientConfig, stsClient);
        }
    }

    private CloudAccount buildAccount(String accountScope, AccountStatus status) {
        CloudAccountCredential credential = CloudAccountCredential.builder()
                .credentialKey("cred-123")
                .accessKeyIdEncrypted("enc-access")
                .secretAccessKeyEncrypted("enc-secret")
                .region("ap-northeast-2")
                .build();

        CloudProvider provider = CloudProvider.builder()
                .providerKey("aws")
                .providerName("AWS")
                .providerType(ProviderType.AWS)
                .build();

        return CloudAccount.builder()
                .tenant(null)
                .provider(provider)
                .accountName("test-account")
                .accountScope(accountScope)
                .credential(credential)
                .accountStatus(status)
                .isDefault(false)
                .build();
    }
}

