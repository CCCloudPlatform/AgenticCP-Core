package com.agenticcp.core.domain.cloud.service.account;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsCredentialManager;
import com.agenticcp.core.domain.cloud.port.model.account.*;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.mapper.CredentialCommandMapper;
import com.agenticcp.core.domain.cloud.service.account.CloudAccountUseCaseService;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountSyncPort;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountValidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountCredentialRepository;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * CloudAccountUseCaseService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudAccountUseCaseService 단위 테스트")
class CloudAccountUseCaseServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @Mock
    private CloudAccountDomainService cloudAccountDomainService;

    @Mock
    private AccountValidationPort accountValidationPort;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CredentialCommandMapper credentialCommandMapper;

    @Mock
    private CloudAccountCredentialRepository cloudAccountCredentialRepository;

    @Mock
    private AuditEventPort auditEventPort;

    @Mock
    private AccountSyncPort accountSyncPort;

    @InjectMocks
    private CloudAccountUseCaseService cloudAccountUseCaseService;

    private String tenantKey;
    private Tenant tenant;
    private CloudProvider provider;
    private RegisterCloudAccountRequest registerRequest;
    private AccountValidationResult validationResult;
    private CloudAccountCredential credential;
    private CloudAccount cloudAccount;

    @BeforeEach
    void setUp() {
        tenantKey = "test-tenant";
        TenantContextHolder.setTenantKey(tenantKey);

        tenant = Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName("Test Tenant")
                .build();
        tenant.setId(1L);

        provider = CloudProvider.builder()
                .providerType(ProviderType.AWS)
                .providerName("Amazon Web Services")
                .build();
        provider.setId(1L);

        registerRequest = RegisterCloudAccountRequest.builder()
                .providerType(ProviderType.AWS)
                .accountName("Test AWS Account")
                .accountScope("123456789012")
                .accessKey("AKIAIOSFODNN7EXAMPLE")
                .secretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .region("us-east-1")
                .isDefault(false)
                .build();

        validationResult = AccountValidationResult.builder()
                .valid(true)
                .message("Validation successful")
                .accountScope("123456789012")
                .region("us-east-1")
                .metadata(Map.of("arn", "arn:aws:iam::123456789012:root"))
                .build();

        credential = CloudAccountCredential.builder()
                .credentialKey("credential-uuid")
                .accessKeyIdEncrypted("encrypted-access-key")
                .secretAccessKeyEncrypted("encrypted-secret-key")
                .region("us-east-1")
                .build();
        credential.setId(1L);

        cloudAccount = CloudAccount.builder()
                .tenant(tenant)
                .provider(provider)
                .accountName("Test AWS Account")
                .accountScope("123456789012")
                .credential(credential)
                .accountStatus(AccountStatus.VERIFIED)
                .isDefault(false)
                .verifiedAt(LocalDateTime.now())
                .build();
        cloudAccount.setId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("클라우드 계정 등록 테스트")
    class RegisterCloudAccountTest {

        @Test
        @DisplayName("새 계정 등록 성공")
        void registerCloudAccount_Success() {
            // given
            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudProviderRepository.findByProviderType(ProviderType.AWS))
                    .willReturn(List.of(provider));
            doNothing().when(cloudAccountDomainService)
                    .validateAccountUniqueness(anyLong(), anyString(), any(ProviderType.class));
            given(accountValidationPort.validateAccount(any(AccountValidationRequest.class)))
                    .willReturn(validationResult);
            given(credentialCommandMapper.toStoreCommand(anyString(), any(ProviderType.class), anyString(), anyString(), anyString(), anyString()))
                    .willReturn(StoreCredentialCommand.builder()
                            .tenantKey(tenantKey)
                            .providerType(ProviderType.AWS)
                            .accountScope("123456789012")
                            .credentials(Map.of("accessKeyId", "AKIAIOSFODNN7EXAMPLE", "secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"))
                            .build());
            given(accountCredentialManagementPort.storeCredentials(anyString(), any(ProviderType.class), anyString(), anyMap()))
                    .willReturn(credential.getCredentialKey());
            given(cloudAccountCredentialRepository.findByCredentialKey(credential.getCredentialKey()))
                    .willReturn(Optional.of(credential));
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(cloudAccount);
            doNothing().when(auditEventPort).record(anyString(), anyString(), anyString(), anyMap());

            // when
            CloudAccountDto result = cloudAccountUseCaseService.registerCloudAccount(registerRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccountName()).isEqualTo("Test AWS Account");
            assertThat(result.getAccountScope()).isEqualTo("123456789012");
            assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.VERIFIED);

            // verify
            then(tenantRepository).should(times(1)).findByTenantKey(tenantKey);
            then(cloudProviderRepository).should(times(1)).findByProviderType(ProviderType.AWS);
            then(cloudAccountDomainService).should(times(1))
                    .validateAccountUniqueness(tenant.getId(), registerRequest.getAccountScope(), ProviderType.AWS);
            then(accountValidationPort).should(times(1)).validateAccount(any(AccountValidationRequest.class));
            then(credentialCommandMapper).should(times(1)).toStoreCommand(anyString(), any(ProviderType.class), anyString(), anyString(), anyString(), anyString());
            then(accountCredentialManagementPort).should(times(1)).storeCredentials(anyString(), any(ProviderType.class), anyString(), anyMap());
            then(cloudAccountCredentialRepository).should(times(1)).findByCredentialKey(credential.getCredentialKey());
            then(cloudAccountRepository).should(times(1)).save(any(CloudAccount.class));
            then(auditEventPort).should(times(1)).record(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("계정 검증 실패 시 예외 발생")
        void registerCloudAccount_ValidationFailed_ThrowsException() {
            // given
            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudProviderRepository.findByProviderType(ProviderType.AWS))
                    .willReturn(List.of(provider));
            doNothing().when(cloudAccountDomainService)
                    .validateAccountUniqueness(anyLong(), anyString(), any(ProviderType.class));

            AccountValidationResult failedValidation = AccountValidationResult.builder()
                    .valid(false)
                    .message("Invalid credentials")
                    .build();
            given(accountValidationPort.validateAccount(any(AccountValidationRequest.class)))
                    .willReturn(failedValidation);

            // when & then
            assertThatThrownBy(() -> cloudAccountUseCaseService.registerCloudAccount(registerRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid credentials")
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_VERIFICATION_FAILED);

            // verify
            then(accountCredentialManagementPort).should(never()).storeCredentials(anyString(), any(ProviderType.class), anyString(), anyMap());
            then(cloudAccountRepository).should(never()).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("중복 계정 등록 시 예외 발생")
        void registerCloudAccount_DuplicateAccount_ThrowsException() {
            // given
            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudProviderRepository.findByProviderType(ProviderType.AWS))
                    .willReturn(List.of(provider));
            doThrow(new BusinessException(CloudErrorCode.DUPLICATE_ACCOUNT, "이미 등록된 클라우드 계정입니다."))
                    .when(cloudAccountDomainService)
                    .validateAccountUniqueness(anyLong(), anyString(), any(ProviderType.class));

            // when & then
            assertThatThrownBy(() -> cloudAccountUseCaseService.registerCloudAccount(registerRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 등록된 클라우드 계정입니다")
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.DUPLICATE_ACCOUNT);

            // verify
            then(accountValidationPort).should(never()).validateAccount(any(AccountValidationRequest.class));
            then(cloudAccountRepository).should(never()).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("기본 계정으로 등록 시 기존 기본 계정 해제")
        void registerCloudAccount_AsDefault_UnsetsExistingDefault() {
            // given
            registerRequest.setIsDefault(true);

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudProviderRepository.findByProviderType(ProviderType.AWS))
                    .willReturn(List.of(provider));
            doNothing().when(cloudAccountDomainService)
                    .validateAccountUniqueness(anyLong(), anyString(), any(ProviderType.class));
            given(accountValidationPort.validateAccount(any(AccountValidationRequest.class)))
                    .willReturn(validationResult);
            given(credentialCommandMapper.toStoreCommand(anyString(), any(ProviderType.class), anyString(), anyString(), anyString(), anyString()))
                    .willReturn(StoreCredentialCommand.builder()
                            .tenantKey(tenantKey)
                            .providerType(ProviderType.AWS)
                            .accountScope("123456789012")
                            .credentials(Map.of("accessKeyId", "AKIAIOSFODNN7EXAMPLE", "secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"))
                            .build());
            given(accountCredentialManagementPort.storeCredentials(anyString(), any(ProviderType.class), anyString(), anyMap()))
                    .willReturn(credential.getCredentialKey());
            given(cloudAccountCredentialRepository.findByCredentialKey(credential.getCredentialKey()))
                    .willReturn(Optional.of(credential));
            doNothing().when(cloudAccountDomainService)
                    .handleDefaultAccountSetting(anyLong(), any(ProviderType.class));
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(cloudAccount);
            doNothing().when(auditEventPort).record(anyString(), anyString(), anyString(), anyMap());

            // when
            cloudAccountUseCaseService.registerCloudAccount(registerRequest);

            // then
            then(cloudAccountDomainService).should(times(1))
                    .handleDefaultAccountSetting(tenant.getId(), ProviderType.AWS);
        }
    }

    @Nested
    @DisplayName("계정 수정 테스트")
    class UpdateCloudAccountTest {

        @Test
        @DisplayName("계정 정보 수정 성공")
        void updateCloudAccount_Success() {
            // given
            Long accountId = 1L;
            UpdateCloudAccountRequest updateRequest = UpdateCloudAccountRequest.builder()
                    .accountName("Updated Account Name")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId()))
                    .willReturn(Optional.of(cloudAccount));
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(cloudAccount);
            doNothing().when(auditEventPort).record(anyString(), anyString(), anyString(), anyMap());

            // when
            CloudAccountDto result = cloudAccountUseCaseService.updateCloudAccount(accountId, updateRequest);

            // then
            assertThat(result).isNotNull();
            then(cloudAccountRepository).should(times(1)).save(any(CloudAccount.class));
            then(auditEventPort).should(times(1)).record(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("존재하지 않는 계정 수정 시 예외 발생")
        void updateCloudAccount_NotFound_ThrowsException() {
            // given
            Long accountId = 999L;
            UpdateCloudAccountRequest updateRequest = UpdateCloudAccountRequest.builder()
                    .accountName("Updated Account Name")
                    .build();

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cloudAccountUseCaseService.updateCloudAccount(accountId, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("계정을 찾을 수 없습니다")
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("계정 삭제 테스트")
    class DeleteCloudAccountTest {

        @Test
        @DisplayName("계정 삭제 성공")
        void deleteCloudAccount_Success() {
            // given
            Long accountId = 1L;

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId()))
                    .willReturn(Optional.of(cloudAccount));
            doNothing().when(cloudAccountDomainService).validateAccountDeletion(any(CloudAccount.class));
            given(credentialCommandMapper.toDeleteCommand(any(ProviderType.class), anyString()))
                    .willReturn(DeleteCredentialCommand.builder()
                            .providerType(ProviderType.AWS)
                            .credentialKey(credential.getCredentialKey())
                            .build());
            doNothing().when(accountCredentialManagementPort).deleteCredentials(any(ProviderType.class), anyString());
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(cloudAccount);
            doNothing().when(auditEventPort).record(anyString(), anyString(), anyString(), anyMap());

            // when
            cloudAccountUseCaseService.deleteCloudAccount(accountId);

            // then
            then(cloudAccountRepository).should(times(1)).save(any(CloudAccount.class));
            then(credentialCommandMapper).should(times(1)).toDeleteCommand(ProviderType.AWS, credential.getCredentialKey());
            then(accountCredentialManagementPort).should(times(1)).deleteCredentials(ProviderType.AWS, credential.getCredentialKey());
            then(auditEventPort).should(times(1)).record(anyString(), anyString(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("연결 테스트")
    class TestConnectionTest {

        @Test
        @DisplayName("연결 테스트 성공")
        void testConnection_Success() {
            // given
            Long accountId = 1L;
            ConnectionTestResult expectedResult = ConnectionTestResult.builder()
                    .success(true)
                    .message("Connection successful")
                    .accountId(accountId)
                    .testedAt(LocalDateTime.now())
                    .build();

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId()))
                    .willReturn(Optional.of(cloudAccount));
            given(credentialCommandMapper.toResolveCommand(anyString(), any(ProviderType.class), anyString()))
                    .willReturn(ResolveCredentialCommand.builder()
                            .tenantKey(tenantKey)
                            .providerType(ProviderType.AWS)
                            .accountScope(credential.getCredentialKey())
                            .build());
            AwsCredentialManager.AwsCredentials awsCredentials = AwsCredentialManager.AwsCredentials.builder()
                    .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                    .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                    .region("us-east-1")
                    .build();
            given(accountCredentialManagementPort.resolveCredentials(anyString(), any(ProviderType.class), anyString()))
                    .willReturn(awsCredentials);
            given(accountValidationPort.testConnection(eq(accountId), anyMap()))
                    .willReturn(expectedResult);

            // when
            ConnectionTestResult result = cloudAccountUseCaseService.testConnection(accountId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Connection successful");
        }

        @Test
        @DisplayName("연결 테스트 실패")
        void testConnection_Failed() {
            // given
            Long accountId = 1L;
            ConnectionTestResult failedResult = ConnectionTestResult.builder()
                    .success(false)
                    .message("Connection failed")
                    .accountId(accountId)
                    .testedAt(LocalDateTime.now())
                    .build();

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId()))
                    .willReturn(Optional.of(cloudAccount));
            given(credentialCommandMapper.toResolveCommand(anyString(), any(ProviderType.class), anyString()))
                    .willReturn(ResolveCredentialCommand.builder()
                            .tenantKey(tenantKey)
                            .providerType(ProviderType.AWS)
                            .accountScope(credential.getCredentialKey())
                            .build());
            AwsCredentialManager.AwsCredentials awsCredentials = AwsCredentialManager.AwsCredentials.builder()
                    .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                    .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                    .region("us-east-1")
                    .build();
            given(accountCredentialManagementPort.resolveCredentials(anyString(), any(ProviderType.class), anyString()))
                    .willReturn(awsCredentials);
            given(accountValidationPort.testConnection(eq(accountId), anyMap()))
                    .willReturn(failedResult);

            // when
            ConnectionTestResult result = cloudAccountUseCaseService.testConnection(accountId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Connection failed");
        }
    }

    @Nested
    @DisplayName("계정 정보 동기화 테스트")
    class SyncAccountInfoTest {

        @Test
        @DisplayName("계정 정보 동기화 성공")
        void syncAccountInfo_Success() {
            // given
            Long accountId = 1L;
            CloudAccount syncedAccount = CloudAccount.builder()
                    .tenant(tenant)
                    .provider(provider)
                    .accountName("Synced Account")
                    .accountScope("123456789012")
                    .credential(credential)
                    .accountStatus(AccountStatus.ACTIVE)
                    .lastSyncAt(LocalDateTime.now())
                    .build();
            syncedAccount.setId(accountId);

            given(tenantRepository.findByTenantKey(tenantKey)).willReturn(Optional.of(tenant));
            given(cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId()))
                    .willReturn(Optional.of(cloudAccount));
            given(accountSyncPort.syncAccountInfo(accountId)).willReturn(syncedAccount);

            // when
            CloudAccountDto result = cloudAccountUseCaseService.syncAccountInfo(accountId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccountName()).isEqualTo("Synced Account");
            then(accountSyncPort).should(times(1)).syncAccountInfo(accountId);
        }
    }

    @Nested
    @DisplayName("계정 사전 검증 테스트")
    class ValidateAccountBeforeRegistrationTest {

        @Test
        @DisplayName("계정 사전 검증 성공")
        void validateAccountBeforeRegistration_Success() {
            // given
            AccountValidationRequest validationRequest = AccountValidationRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                    .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                    .region("us-east-1")
                    .build();

            given(accountValidationPort.validateAccount(validationRequest))
                    .willReturn(validationResult);

            // when
            AccountValidationResult result = 
                    cloudAccountUseCaseService.validateAccountBeforeRegistration(validationRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getValid()).isTrue();
            assertThat(result.getAccountScope()).isEqualTo("123456789012");
        }
    }
}

