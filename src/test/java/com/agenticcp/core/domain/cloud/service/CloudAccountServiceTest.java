package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CloudAccountService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudAccountService 단위 테스트")
class CloudAccountServiceTest {

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @InjectMocks
    private CloudAccountService cloudAccountService;

    private Tenant testTenant;
    private CloudProvider awsProvider;
    private CloudProvider gcpProvider;
    private CloudProvider azureProvider;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .build();
        testTenant.setId(1L);

        awsProvider = CloudProvider.builder()
                .providerKey("aws")
                .providerName("Amazon Web Services")
                .providerType(CloudProvider.ProviderType.AWS)
                .build();
        awsProvider.setId(1L);

        gcpProvider = CloudProvider.builder()
                .providerKey("gcp")
                .providerName("Google Cloud Platform")
                .providerType(CloudProvider.ProviderType.GCP)
                .build();
        gcpProvider.setId(2L);

        azureProvider = CloudProvider.builder()
                .providerKey("azure")
                .providerName("Microsoft Azure")
                .providerType(CloudProvider.ProviderType.AZURE)
                .build();
        azureProvider.setId(3L);
    }

    @Nested
    @DisplayName("AWS 계정 생성 테스트")
    class CreateAwsAccountTest {

        @Test
        @DisplayName("AWS 계정 생성 성공")
        void createAwsAccount_WithValidData_Success() {
            // Given
            CloudAccount awsAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .accountName("aws-prod")
                    .roleArn("arn:aws:iam::123456789012:role/AgenticCPRole")
                    .externalId("external-id-123")
                    .defaultRegion("us-east-1")
                    .status(Status.ACTIVE)
                    .isDefault(false)
                    .build();

            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(1L, 1L, "123456789012"))
                    .thenReturn(false);
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(awsAccount);

            // When
            CloudAccount result = cloudAccountService.createAccount(awsAccount);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo("123456789012");
            assertThat(result.getRoleArn()).isEqualTo("arn:aws:iam::123456789012:role/AgenticCPRole");
            assertThat(result.getDefaultRegion()).isEqualTo("us-east-1");
            assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);

            verify(cloudAccountRepository).existsByTenantIdAndProviderIdAndAccountId(1L, 1L, "123456789012");
            verify(cloudAccountRepository).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("중복된 AWS 계정 생성 시 예외 발생")
        void createAwsAccount_WithDuplicateAccount_ThrowsException() {
            // Given
            CloudAccount awsAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .accountName("aws-prod")
                    .build();

            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(1L, 1L, "123456789012"))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.createAccount(awsAccount))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 등록된 클라우드 계정입니다");

            verify(cloudAccountRepository).existsByTenantIdAndProviderIdAndAccountId(1L, 1L, "123456789012");
            verify(cloudAccountRepository, never()).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("잘못된 AWS Account ID 형식으로 생성 시 예외 발생")
        void createAwsAccount_WithInvalidAccountIdFormat_ThrowsException() {
            // Given
            CloudAccount awsAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("12345")  // 12자리가 아님
                    .accountName("aws-prod")
                    .build();

            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(1L, 1L, "12345"))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.createAccount(awsAccount))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("GCP 계정 생성 테스트")
    class CreateGcpAccountTest {

        @Test
        @DisplayName("GCP 계정 생성 성공")
        void createGcpAccount_WithValidData_Success() {
            // Given
            CloudAccount gcpAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(gcpProvider)
                    .accountId("my-gcp-project")
                    .accountName("gcp-prod")
                    .serviceAccountEmail("service@my-gcp-project.iam.gserviceaccount.com")
                    .defaultRegion("us-central1")
                    .status(Status.ACTIVE)
                    .isDefault(false)
                    .build();

            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(1L, 2L, "my-gcp-project"))
                    .thenReturn(false);
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(gcpAccount);

            // When
            CloudAccount result = cloudAccountService.createAccount(gcpAccount);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo("my-gcp-project");
            assertThat(result.getServiceAccountEmail()).isEqualTo("service@my-gcp-project.iam.gserviceaccount.com");
            assertThat(result.getDefaultRegion()).isEqualTo("us-central1");

            verify(cloudAccountRepository).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("잘못된 GCP Project ID 형식으로 생성 시 예외 발생")
        void createGcpAccount_WithInvalidProjectIdFormat_ThrowsException() {
            // Given - GCP Project ID는 6-30자, 소문자/숫자/하이픈만 가능
            String invalidProjectId = "MY_GCP_PROJECT";  // 언더스코어 사용 불가

            // When & Then
            assertThatCode(() -> cloudAccountService.validateGcpProjectId(invalidProjectId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Azure 계정 생성 테스트")
    class CreateAzureAccountTest {

        @Test
        @DisplayName("Azure 계정 생성 성공")
        void createAzureAccount_WithValidData_Success() {
            // Given
            CloudAccount azureAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(azureProvider)
                    .accountId("12345678-1234-1234-1234-123456789012")
                    .accountName("azure-prod")
                    .azureTenantId("87654321-4321-4321-4321-210987654321")
                    .azureClientId("abcdef12-3456-7890-abcd-ef1234567890")
                    .defaultRegion("eastus")
                    .status(Status.ACTIVE)
                    .isDefault(false)
                    .build();

            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(
                    1L, 3L, "12345678-1234-1234-1234-123456789012"))
                    .thenReturn(false);
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(azureAccount);

            // When
            CloudAccount result = cloudAccountService.createAccount(azureAccount);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo("12345678-1234-1234-1234-123456789012");
            assertThat(result.getAzureTenantId()).isEqualTo("87654321-4321-4321-4321-210987654321");
            assertThat(result.getAzureClientId()).isEqualTo("abcdef12-3456-7890-abcd-ef1234567890");

            verify(cloudAccountRepository).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("잘못된 Azure Subscription ID 형식으로 생성 시 예외 발생")
        void createAzureAccount_WithInvalidSubscriptionIdFormat_ThrowsException() {
            // Given - Azure Subscription ID는 UUID 형식이어야 함
            String invalidSubscriptionId = "not-a-valid-uuid";

            // When & Then
            assertThatCode(() -> cloudAccountService.validateAzureSubscriptionId(invalidSubscriptionId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("계정 조회 테스트")
    class GetAccountTest {

        @Test
        @DisplayName("ID로 계정 조회 성공")
        void getAccountById_WithExistingId_ReturnsAccount() {
            // Given
            CloudAccount account = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .accountName("aws-prod")
                    .build();
            account.setId(1L);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(account));

            // When
            CloudAccount result = cloudAccountService.getAccountByIdOrThrow(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAccountId()).isEqualTo("123456789012");

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(1L);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외 발생")
        void getAccountById_WithNonExistingId_ThrowsException() {
            // Given
            when(cloudAccountRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.getAccountByIdOrThrow(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(999L);
        }

        @Test
        @DisplayName("테넌트별 계정 목록 조회")
        void getAccountsByTenantId_ReturnsAccountList() {
            // Given
            List<CloudAccount> accounts = List.of(
                    CloudAccount.builder()
                            .tenant(testTenant)
                            .provider(awsProvider)
                            .accountId("123456789012")
                            .accountName("aws-prod")
                            .build(),
                    CloudAccount.builder()
                            .tenant(testTenant)
                            .provider(gcpProvider)
                            .accountId("my-gcp-project")
                            .accountName("gcp-prod")
                            .build()
            );

            when(cloudAccountRepository.findByTenantId(1L)).thenReturn(accounts);

            // When
            List<CloudAccount> result = cloudAccountService.getAccountsByTenantId(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CloudAccount::getAccountName)
                    .containsExactly("aws-prod", "gcp-prod");

            verify(cloudAccountRepository).findByTenantId(1L);
        }
    }

    @Nested
    @DisplayName("계정 수정 테스트")
    class UpdateAccountTest {

        @Test
        @DisplayName("계정 정보 수정 성공")
        void updateAccount_WithValidData_Success() {
            // Given
            CloudAccount existingAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .accountName("aws-prod-old")
                    .defaultRegion("us-east-1")
                    .status(Status.ACTIVE)
                    .isDefault(false)
                    .build();
            existingAccount.setId(1L);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existingAccount));
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(existingAccount);

            // When
            existingAccount.setAccountName("aws-prod-new");
            existingAccount.setDefaultRegion("ap-northeast-2");
            CloudAccount result = cloudAccountService.updateAccount(1L, existingAccount);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccountName()).isEqualTo("aws-prod-new");
            assertThat(result.getDefaultRegion()).isEqualTo("ap-northeast-2");

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(1L);
            verify(cloudAccountRepository).save(existingAccount);
        }

        @Test
        @DisplayName("존재하지 않는 계정 수정 시 예외 발생")
        void updateAccount_WithNonExistingId_ThrowsException() {
            // Given
            when(cloudAccountRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            CloudAccount updateData = CloudAccount.builder()
                    .accountName("new-name")
                    .build();

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.updateAccount(999L, updateData))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(999L);
            verify(cloudAccountRepository, never()).save(any(CloudAccount.class));
        }
    }

    @Nested
    @DisplayName("계정 삭제 테스트")
    class DeleteAccountTest {

        @Test
        @DisplayName("계정 소프트 삭제 성공")
        void deleteAccount_WithValidId_Success() {
            // Given
            CloudAccount account = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .accountName("aws-prod")
                    .build();
            account.setId(1L);
            account.setIsDeleted(false);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(account));
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(account);

            // When
            cloudAccountService.deleteAccount(1L);

            // Then
            assertThat(account.getIsDeleted()).isTrue();

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(1L);
            verify(cloudAccountRepository).save(account);
        }

        @Test
        @DisplayName("리소스가 있는 계정 삭제 시 예외 발생")
        void deleteAccount_WithAssociatedResources_ThrowsException() {
            // Given
            CloudAccount account = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .build();
            account.setId(1L);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(account));
            // TODO: CloudResource 연동 후 hasResources 체크 로직 추가

            // When & Then
            // TODO: 실제 구현 시 리소스 존재 여부 검증 추가
            cloudAccountService.deleteAccount(1L);

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(1L);
        }
    }

    @Nested
    @DisplayName("계정 상태 전이 테스트")
    class AccountStatusTransitionTest {

        @Test
        @DisplayName("PENDING에서 ACTIVE로 상태 전이 성공")
        void transitionStatus_FromPendingToActive_Success() {
            // Given
            CloudAccount account = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .status(Status.PENDING)
                    .build();
            account.setId(1L);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(account));
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(account);

            // When
            account.setStatus(Status.ACTIVE);
            CloudAccount result = cloudAccountService.updateAccount(1L, account);

            // Then
            assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);

            verify(cloudAccountRepository).save(account);
        }

        @Test
        @DisplayName("ACTIVE에서 SUSPENDED로 상태 전이 성공")
        void transitionStatus_FromActiveToSuspended_Success() {
            // Given
            CloudAccount account = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .status(Status.ACTIVE)
                    .build();
            account.setId(1L);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(account));
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(account);

            // When
            account.setStatus(Status.SUSPENDED);
            CloudAccount result = cloudAccountService.updateAccount(1L, account);

            // Then
            assertThat(result.getStatus()).isEqualTo(Status.SUSPENDED);

            verify(cloudAccountRepository).save(account);
        }
    }

    @Nested
    @DisplayName("계정 검증 로직 테스트")
    class AccountValidationTest {

        @Test
        @DisplayName("계정 유일성 검증 - 중복 없음")
        void validateAccountUniqueness_WithUniqueAccount_Success() {
            // Given
            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(
                    1L, 1L, "123456789012")).thenReturn(false);

            // When & Then
            assertThatCode(() -> cloudAccountService.validateAccountUniqueness(
                    1L, 1L, "123456789012"))
                    .doesNotThrowAnyException();

            verify(cloudAccountRepository).existsByTenantIdAndProviderIdAndAccountId(
                    1L, 1L, "123456789012");
        }

        @Test
        @DisplayName("계정 유일성 검증 - 중복 존재")
        void validateAccountUniqueness_WithDuplicateAccount_ThrowsException() {
            // Given
            when(cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(
                    1L, 1L, "123456789012")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.validateAccountUniqueness(
                    1L, 1L, "123456789012"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 등록된 클라우드 계정입니다");

            verify(cloudAccountRepository).existsByTenantIdAndProviderIdAndAccountId(
                    1L, 1L, "123456789012");
        }

        @Test
        @DisplayName("AWS Account ID 형식 검증 - 올바른 형식")
        void validateAwsAccountId_WithValidFormat_Success() {
            // Given
            String validAccountId = "123456789012";

            // When & Then
            assertThatCode(() -> cloudAccountService.validateAwsAccountId(validAccountId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("AWS Account ID 형식 검증 - 잘못된 형식 (길이)")
        void validateAwsAccountId_WithInvalidLength_ThrowsException() {
            // Given
            String invalidAccountId = "12345";  // 12자리가 아님

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.validateAwsAccountId(invalidAccountId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("AWS Account ID 형식 검증 - 잘못된 형식 (숫자 아님)")
        void validateAwsAccountId_WithNonNumeric_ThrowsException() {
            // Given
            String invalidAccountId = "12345678901a";  // 문자 포함

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.validateAwsAccountId(invalidAccountId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("GCP Project ID 형식 검증 - 올바른 형식")
        void validateGcpProjectId_WithValidFormat_Success() {
            // Given
            String validProjectId = "my-gcp-project-123";

            // When & Then
            assertThatCode(() -> cloudAccountService.validateGcpProjectId(validProjectId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GCP Project ID 형식 검증 - 잘못된 형식 (대문자)")
        void validateGcpProjectId_WithUpperCase_ThrowsException() {
            // Given
            String invalidProjectId = "MY-GCP-PROJECT";

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.validateGcpProjectId(invalidProjectId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("GCP Project ID 형식 검증 - 잘못된 형식 (길이 초과)")
        void validateGcpProjectId_WithTooLong_ThrowsException() {
            // Given
            String invalidProjectId = "a".repeat(31);  // 30자 초과

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.validateGcpProjectId(invalidProjectId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Azure Subscription ID 형식 검증 - 올바른 형식")
        void validateAzureSubscriptionId_WithValidFormat_Success() {
            // Given
            String validSubscriptionId = "12345678-1234-1234-1234-123456789012";

            // When & Then
            assertThatCode(() -> cloudAccountService.validateAzureSubscriptionId(validSubscriptionId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Azure Subscription ID 형식 검증 - 잘못된 형식")
        void validateAzureSubscriptionId_WithInvalidFormat_ThrowsException() {
            // Given
            String invalidSubscriptionId = "not-a-valid-uuid";

            // When & Then
            assertThatThrownBy(() -> cloudAccountService.validateAzureSubscriptionId(invalidSubscriptionId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("기본 계정 설정 테스트")
    class DefaultAccountTest {

        @Test
        @DisplayName("프로바이더별 기본 계정 조회")
        void getDefaultAccountByProvider_ReturnsDefaultAccount() {
            // Given
            CloudAccount defaultAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .accountName("aws-default")
                    .isDefault(true)
                    .build();
            defaultAccount.setId(1L);

            when(cloudAccountRepository.findByTenantIdAndProviderIdAndIsDefaultTrue(1L, 1L))
                    .thenReturn(Optional.of(defaultAccount));

            // When
            Optional<CloudAccount> result = cloudAccountService.getDefaultAccountByTenantIdAndProviderId(1L, 1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getIsDefault()).isTrue();
            assertThat(result.get().getAccountName()).isEqualTo("aws-default");

            verify(cloudAccountRepository).findByTenantIdAndProviderIdAndIsDefaultTrue(1L, 1L);
        }

        @Test
        @DisplayName("기본 계정으로 설정")
        void setAsDefaultAccount_Success() {
            // Given
            CloudAccount account = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("123456789012")
                    .isDefault(false)
                    .build();
            account.setId(1L);

            CloudAccount oldDefaultAccount = CloudAccount.builder()
                    .tenant(testTenant)
                    .provider(awsProvider)
                    .accountId("210987654321")
                    .isDefault(true)
                    .build();
            oldDefaultAccount.setId(2L);

            when(cloudAccountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(account));
            when(cloudAccountRepository.findByTenantIdAndProviderIdAndIsDefaultTrue(1L, 1L))
                    .thenReturn(Optional.of(oldDefaultAccount));
            when(cloudAccountRepository.save(any(CloudAccount.class))).thenReturn(account);

            // When
            cloudAccountService.setAsDefaultAccount(1L);

            // Then
            assertThat(account.getIsDefault()).isTrue();
            assertThat(oldDefaultAccount.getIsDefault()).isFalse();

            verify(cloudAccountRepository).findByIdAndIsDeletedFalse(1L);
            verify(cloudAccountRepository).findByTenantIdAndProviderIdAndIsDefaultTrue(1L, 1L);
            verify(cloudAccountRepository, times(2)).save(any(CloudAccount.class));
        }
    }
}

