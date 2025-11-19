package com.agenticcp.core.domain.cloud.adapter.outbound;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsSessionProvider;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsCredentialManager;
import com.agenticcp.core.domain.cloud.service.SessionCacheService;
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

/**
 * CredentialProviderPortAdapter의 getSession 메서드 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialProviderPortAdapter getSession 테스트")
class CredentialProviderPortAdapterGetSessionTest {

    @Mock
    private AwsCredentialManager awsCredentialManager;

    @Mock
    private AwsSessionProvider awsSessionProvider;

    @Mock
    private SessionCacheService sessionCacheService;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    private CredentialProviderPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CredentialProviderPortAdapter(
                awsCredentialManager,
                awsSessionProvider,
                sessionCacheService,
                cloudAccountRepository
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
            Long accountId = 1L;
            ProviderType providerType = ProviderType.AWS;

            AwsSessionCredential cachedSession = AwsSessionCredential.builder()
                    .accessKeyId("cached-access-key")
                    .secretAccessKey("cached-secret-key")
                    .sessionToken("cached-session-token")
                    .region("us-east-1")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            when(sessionCacheService.getCachedSession(tenantKey, accountId, providerType))
                    .thenReturn(Optional.of(cachedSession));

            // when
            CloudSessionCredential result = adapter.getSession(tenantKey, accountId, providerType);

            // then
            assertThat(result).isEqualTo(cachedSession);
            verify(awsSessionProvider, never()).getSession(anyString(), anyInt());
            verify(sessionCacheService, never()).cacheSession(anyString(), anyLong(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("캐시에 없으면 새로 발급하고 캐싱")
        void getSession_NoCache_IssuesAndCaches() {
            // given
            String tenantKey = "tenant-1";
            Long accountId = 1L;
            ProviderType providerType = ProviderType.AWS;
            String credentialKey = "credential-key-123";

            CloudAccount account = CloudAccount.builder()
                    .credential(CloudAccountCredential.builder()
                            .credentialKey(credentialKey)
                            .build())
                    .build();
            account.setId(accountId);

            when(sessionCacheService.getCachedSession(tenantKey, accountId, providerType))
                    .thenReturn(Optional.empty());
            when(cloudAccountRepository.findById(accountId))
                    .thenReturn(Optional.of(account));

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
            CloudSessionCredential result = adapter.getSession(tenantKey, accountId, providerType);

            // then
            assertThat(result).isEqualTo(newSession);
            verify(awsSessionProvider).getSession(credentialKey, 3600);
            verify(sessionCacheService).cacheSession(eq(tenantKey), eq(accountId), eq(providerType), 
                    eq(newSession), anyInt());
        }

        @Test
        @DisplayName("계정을 찾을 수 없으면 BusinessException 발생")
        void getSession_AccountNotFound_ThrowsException() {
            // given
            String tenantKey = "tenant-1";
            Long accountId = 999L;
            ProviderType providerType = ProviderType.AWS;

            when(sessionCacheService.getCachedSession(tenantKey, accountId, providerType))
                    .thenReturn(Optional.empty());
            when(cloudAccountRepository.findById(accountId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adapter.getSession(tenantKey, accountId, providerType))
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
            Long accountId = 1L;
            ProviderType providerType = ProviderType.AZURE;

            when(sessionCacheService.getCachedSession(tenantKey, accountId, providerType))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adapter.getSession(tenantKey, accountId, providerType))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.UNSUPPORTED_OPERATION);
                    });
        }
    }
}

