package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.SessionCachePort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsAccountCredentialManagementAdapter 테스트")
class AwsAccountCredentialManagementAdapterTest {

    @Mock
    private AwsCredentialManager awsCredentialManager;

    @Mock
    private AwsSessionProvider awsSessionProvider;

    @Mock
    private SessionCachePort sessionCachePort;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @Mock
    private MaskingService maskingService;

    private AwsAccountCredentialManagementAdapter adapter;

    @BeforeEach
    void setUp() {
        // MaskingService는 외부 의존성이므로 Mock으로 처리
        // 실제 마스킹 로직 검증은 MaskingService 및 MaskingStrategy의 단위 테스트에서 수행
        // lenient로 설정하여 일부 테스트에서 사용하지 않는 stubbing 경고 방지
        lenient().when(maskingService.maskTenantKey(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(maskingService.maskAccountScope(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        
        adapter = new AwsAccountCredentialManagementAdapter(
                awsCredentialManager,
                awsSessionProvider,
                sessionCachePort,
                cloudAccountRepository,
                maskingService
        );
    }

    @Nested
    @DisplayName("세션 획득 테스트")
    class GetSessionTest {

        @Test
        @DisplayName("캐시된 세션이 있으면 캐시에서 반환")
        void getSession_CachedSession_ReturnsCached() {
            // given
            String tenantKey = "tenant-1";
            String accountScope = "123456789012";
            ProviderType providerType = ProviderType.AWS;

            AwsSessionCredential cachedSession = AwsSessionCredential.builder()
                    .accessKeyId("cached-access-key")
                    .secretAccessKey("cached-secret-key")
                    .sessionToken("cached-session-token")
                    .region("us-east-1")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            when(sessionCachePort.getCachedSession(tenantKey, accountScope, providerType))
                    .thenReturn(Optional.of(cachedSession));

            // when
            CloudSessionCredential result = adapter.getSession(tenantKey, accountScope, providerType);

            // then
            assertThat(result).isEqualTo(cachedSession);
            verify(awsSessionProvider, never()).getSession(anyString(), anyInt());
            verify(sessionCachePort, never()).cacheSession(anyString(), anyString(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("캐시에 없으면 새로 발급하고 캐싱")
        void getSession_NoCache_IssuesAndCaches() {
            // given
            String tenantKey = "tenant-1";
            String accountScope = "123456789012";
            ProviderType providerType = ProviderType.AWS;
            String credentialKey = "credential-key-123";

            CloudAccount account = CloudAccount.builder()
                    .accountScope(accountScope)
                    .credential(CloudAccountCredential.builder()
                            .credentialKey(credentialKey)
                            .build())
                    .build();

            when(sessionCachePort.getCachedSession(tenantKey, accountScope, providerType))
                    .thenReturn(Optional.empty());
            when(cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType))
                    .thenReturn(java.util.List.of(account));

            AwsSessionCredential newSession = AwsSessionCredential.builder()
                    .accessKeyId("new-access-key")
                    .secretAccessKey("new-secret-key")
                    .sessionToken("new-session-token")
                    .region("us-east-1")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            when(awsSessionProvider.getSession(credentialKey, 3600))
                    .thenReturn(newSession);

            // when
            CloudSessionCredential result = adapter.getSession(tenantKey, accountScope, providerType);

            // then
            assertThat(result).isEqualTo(newSession);
            verify(awsSessionProvider).getSession(credentialKey, 3600);
            verify(sessionCachePort).cacheSession(eq(tenantKey), eq(accountScope), eq(providerType),
                    eq(newSession), anyInt());
        }

        @Test
        @DisplayName("계정을 찾을 수 없으면 BusinessException 발생")
        void getSession_AccountNotFound_ThrowsException() {
            // given
            String tenantKey = "tenant-1";
            String accountScope = "999999999999";
            ProviderType providerType = ProviderType.AWS;

            when(sessionCachePort.getCachedSession(tenantKey, accountScope, providerType))
                    .thenReturn(Optional.empty());
            when(cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType))
                    .thenReturn(java.util.List.of());

            // when & then
            assertThatThrownBy(() -> adapter.getSession(tenantKey, accountScope, providerType))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.ACCOUNT_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("지원하지 않는 프로바이더면 BusinessException 발생")
        void getSession_UnsupportedProvider_ThrowsException() {
            // given
            String tenantKey = "tenant-1";
            String accountScope = "123456789012";
            ProviderType providerType = ProviderType.AZURE;

            // when & then
            assertThatThrownBy(() -> adapter.getSession(tenantKey, accountScope, providerType))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.UNSUPPORTED_OPERATION);
                    });
        }
    }
}

