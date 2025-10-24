package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.domain.cloud.dto.*;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.mapper.CloudAccountMapper;
import com.agenticcp.core.domain.cloud.port.model.AccountMetadata;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationResult;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CloudAccountCredentialPort;
import com.agenticcp.core.domain.cloud.port.outbound.CloudAccountValidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CloudAccountUseCaseService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudAccountUseCaseService 테스트")
class CloudAccountUseCaseServiceTest {
    
    @Mock
    private CloudAccountService cloudAccountService;
    
    @Mock
    private CloudProviderService cloudProviderService;
    
    @Mock
    private CloudAccountMapper cloudAccountMapper;
    
    @Mock
    private CloudAccountValidationPort validationPort;
    
    @Mock
    private CloudAccountCredentialPort credentialPort;
    
    @Mock
    private AuditEventPort auditEventPort;
    
    @Mock
    private TracingPort tracingPort;
    
    @InjectMocks
    private CloudAccountUseCaseService useCaseService;
    
    private CloudProvider awsProvider;
    
    @BeforeEach
    void setUp() {
        awsProvider = CloudProvider.builder()
                .providerKey("aws")
                .providerType(ProviderType.AWS)
                .build();
    }
    
    @Nested
    @DisplayName("AWS 계정 등록 테스트")
    class AwsAccountRegistrationTest {
        
        @Test
        @DisplayName("AWS 계정 등록 성공")
        void registerAwsAccount_Success() {
            // given
            RegisterAwsAccountRequest request = RegisterAwsAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(1L)
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .authMethod(CloudAccount.AuthMethod.IAM_ROLE)
                    .roleArn("arn:aws:iam::123456789012:role/MyRole")
                    .externalId("external-id-123")
                    .region("us-east-1")
                    .build();
            
            AccountValidationResult validationResult = AccountValidationResult.success(
                    "123456789012", 
                    ProviderType.AWS, 
                    AccountMetadata.builder()
                            .providerType(ProviderType.AWS)
                            .accountId("123456789012")
                            .accountName("AWS Production Account")
                            .build()
            );
            
            CloudAccount savedAccount = CloudAccount.builder()
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .build();
            
            CloudAccountDto expectedDto = CloudAccountDto.builder()
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .build();
            
            when(cloudProviderService.getProviderByKeyOrThrow("aws")).thenReturn(awsProvider);
            when(validationPort.validateAccount(any(AccountValidationRequest.class))).thenReturn(validationResult);
            when(cloudAccountService.createAccount(any(CloudAccount.class))).thenReturn(savedAccount);
            when(cloudAccountMapper.toDto(savedAccount)).thenReturn(expectedDto);
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            
            // when
            CloudAccountDto result = useCaseService.registerAwsAccount(request);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo("123456789012");
            
            verify(cloudProviderService).getProviderByKeyOrThrow("aws");
            verify(validationPort).validateAccount(any(AccountValidationRequest.class));
            verify(cloudAccountService).createAccount(any(CloudAccount.class));
            verify(auditEventPort).record(eq("CLOUD_ACCOUNT_REGISTERED"), eq("CloudAccount"), eq("SUCCESS"), any(Map.class));
        }
        
        @Test
        @DisplayName("AWS 계정 검증 실패 시 예외 발생")
        void registerAwsAccount_ValidationFailed() {
            // given
            RegisterAwsAccountRequest request = RegisterAwsAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(1L)
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .authMethod(CloudAccount.AuthMethod.IAM_ROLE)
                    .roleArn("arn:aws:iam::123456789012:role/MyRole")
                    .build();
            
            AccountValidationResult validationResult = AccountValidationResult.failure(
                    "123456789012", 
                    ProviderType.AWS, 
                    "Invalid credentials", 
                    "INVALID_CREDENTIALS"
            );
            
            when(cloudProviderService.getProviderByKeyOrThrow("aws")).thenReturn(awsProvider);
            when(validationPort.validateAccount(any(AccountValidationRequest.class))).thenReturn(validationResult);
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            
            // when & then
            assertThatThrownBy(() -> useCaseService.registerAwsAccount(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CloudErrorCode.ACCOUNT_VALIDATION_FAILED);
            
            verify(cloudProviderService).getProviderByKeyOrThrow("aws");
            verify(validationPort).validateAccount(any(AccountValidationRequest.class));
            verify(cloudAccountService, never()).createAccount(any(CloudAccount.class));
        }
    }
    
    @Nested
    @DisplayName("멀티 클라우드 조회 테스트")
    class MultiCloudQueryTest {
        
        @Test
        @DisplayName("멀티 클라우드 계정 조회 성공")
        void getMultiCloudAccounts_Success() {
            // given
            Long tenantId = 1L;
            List<CloudAccount> accounts = List.of(
                    CloudAccount.builder().accountId("aws-account").provider(awsProvider).build(),
                    CloudAccount.builder().accountId("gcp-project").provider(awsProvider).build()
            );
            
            
            when(cloudAccountService.getAccountsByTenantId(tenantId)).thenReturn(accounts);
            when(cloudAccountMapper.toDto(any(CloudAccount.class))).thenAnswer(invocation -> {
                CloudAccount account = invocation.getArgument(0);
                return CloudAccountDto.builder()
                        .accountId(account.getAccountId())
                        .build();
            });
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            
            // when
            List<CloudAccountDto> result = useCaseService.getMultiCloudAccounts(tenantId);
            
            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAccountId()).isEqualTo("aws-account");
            assertThat(result.get(1).getAccountId()).isEqualTo("gcp-project");
            
            verify(cloudAccountService).getAccountsByTenantId(tenantId);
            verify(cloudAccountMapper, times(2)).toDto(any(CloudAccount.class));
        }
    }
    
    @Nested
    @DisplayName("GCP 계정 등록 테스트")
    class GcpAccountRegistrationTest {
        
        @Test
        @DisplayName("GCP 계정 등록은 Phase 7에서 구현 예정")
        void registerGcpAccount_NotImplemented() {
            // given
            RegisterGcpAccountRequest request = RegisterGcpAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(2L)
                    .accountId("my-gcp-project")
                    .accountName("gcp-prod-account")
                    .authMethod(CloudAccount.AuthMethod.SERVICE_ACCOUNT)
                    .serviceAccountEmail("service-account@my-gcp-project.iam.gserviceaccount.com")
                    .projectId("my-gcp-project")
                    .zone("us-central1-a")
                    .build();
            
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            
            // when & then
            assertThatThrownBy(() -> useCaseService.registerGcpAccount(request))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("GCP 계정 등록은 Phase 7에서 구현 예정입니다.");
        }
    }
    
    @Nested
    @DisplayName("Azure 계정 등록 테스트")
    class AzureAccountRegistrationTest {
        
        @Test
        @DisplayName("Azure 계정 등록은 Phase 7에서 구현 예정")
        void registerAzureAccount_NotImplemented() {
            // given
            RegisterAzureAccountRequest request = RegisterAzureAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(3L)
                    .accountId("12345678-1234-1234-1234-123456789012")
                    .accountName("azure-prod-account")
                    .authMethod(CloudAccount.AuthMethod.SERVICE_PRINCIPAL)
                    .azureTenantId("87654321-4321-4321-4321-210987654321")
                    .azureClientId("abcdef12-3456-7890-abcd-ef1234567890")
                    .subscriptionId("12345678-1234-1234-1234-123456789012")
                    .location("eastus")
                    .build();
            
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            
            // when & then
            assertThatThrownBy(() -> useCaseService.registerAzureAccount(request))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Azure 계정 등록은 Phase 7에서 구현 예정입니다.");
        }
    }
    
    @Nested
    @DisplayName("연결 테스트 테스트")
    class ConnectionTestTest {
        
        @Test
        @DisplayName("연결 테스트 성공")
        void testConnection_Success() {
            // given
            Long accountId = 1L;
            CloudAccount mockAccount = CloudAccount.builder()
                    .accountId("123456789012")
                    .provider(CloudProvider.builder()
                            .providerType(ProviderType.AWS)
                            .build())
                    .defaultRegion("us-east-1")
                    .authMethod(CloudAccount.AuthMethod.IAM_ROLE)
                    .roleArn("arn:aws:iam::123456789012:role/MyRole")
                    .externalId("external-id")
                    .build();
            mockAccount.setId(accountId);
            
            AccountValidationResult mockResult = AccountValidationResult.builder()
                    .valid(true)
                    .accountId("123456789012")
                    .providerType(ProviderType.AWS)
                    .message("Connection successful")
                    .build();
            
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            when(cloudAccountService.getAccountByIdOrThrow(accountId)).thenReturn(mockAccount);
            when(validationPort.testConnection(any(AccountValidationRequest.class))).thenReturn(mockResult);
            when(cloudAccountService.updateLastVerified(accountId)).thenReturn(mockAccount);
            
            // when
            ConnectionTestResult result = useCaseService.testConnection(accountId);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isConnected()).isTrue();
            assertThat(result.getAccountId()).isEqualTo(accountId);
            assertThat(result.getCloudAccountId()).isEqualTo("123456789012");
            assertThat(result.getProviderType()).isEqualTo("AWS");
        }
    }
    
    @Nested
    @DisplayName("계정 동기화 테스트")
    class AccountSyncTest {
        
        @Test
        @DisplayName("계정 동기화 성공")
        void syncAllAccounts_Success() {
            // given
            Long tenantId = 1L;
            CloudAccount mockAccount1 = CloudAccount.builder()
                    .accountId("123456789012")
                    .provider(CloudProvider.builder()
                            .providerType(ProviderType.AWS)
                            .build())
                    .defaultRegion("us-east-1")
                    .authMethod(CloudAccount.AuthMethod.IAM_ROLE)
                    .roleArn("arn:aws:iam::123456789012:role/MyRole")
                    .externalId("external-id")
                    .build();
            mockAccount1.setId(1L);
            
            CloudAccount mockAccount2 = CloudAccount.builder()
                    .accountId("987654321098")
                    .provider(CloudProvider.builder()
                            .providerType(ProviderType.AWS)
                            .build())
                    .defaultRegion("us-west-2")
                    .authMethod(CloudAccount.AuthMethod.IAM_ROLE)
                    .roleArn("arn:aws:iam::987654321098:role/MyRole")
                    .externalId("external-id-2")
                    .build();
            mockAccount2.setId(2L);
            
            List<CloudAccount> accounts = List.of(mockAccount1, mockAccount2);
            
            AccountValidationResult mockResult1 = AccountValidationResult.builder()
                    .valid(true)
                    .accountId("123456789012")
                    .providerType(ProviderType.AWS)
                    .message("Connection successful")
                    .build();
            
            AccountValidationResult mockResult2 = AccountValidationResult.builder()
                    .valid(false)
                    .accountId("987654321098")
                    .providerType(ProviderType.AWS)
                    .message("Connection failed")
                    .build();
            
            when(tracingPort.startSpan(anyString(), any(Map.class))).thenReturn(mock(AutoCloseable.class));
            when(cloudAccountService.getAccountsByTenantId(tenantId)).thenReturn(accounts);
            when(cloudAccountService.getAccountByIdOrThrow(1L)).thenReturn(mockAccount1);
            when(cloudAccountService.getAccountByIdOrThrow(2L)).thenReturn(mockAccount2);
            when(validationPort.testConnection(any(AccountValidationRequest.class)))
                    .thenReturn(mockResult1)
                    .thenReturn(mockResult2);
            when(cloudAccountService.updateLastVerified(1L)).thenReturn(mockAccount1);
            
            // when
            Map<String, Object> result = useCaseService.syncAllAccounts(tenantId);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.get("totalAccounts")).isEqualTo(2);
            assertThat(result.get("successCount")).isEqualTo(1);
            assertThat(result.get("failureCount")).isEqualTo(1);
            assertThat(result.get("syncedAt")).isNotNull();
        }
    }
}
