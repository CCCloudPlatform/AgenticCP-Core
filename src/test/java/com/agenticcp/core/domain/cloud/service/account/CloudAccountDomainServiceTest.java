package com.agenticcp.core.domain.cloud.service.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.cloud.service.account.CloudAccountDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * CloudAccountDomainService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudAccountDomainService 단위 테스트")
class CloudAccountDomainServiceTest {

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @InjectMocks
    private CloudAccountDomainService cloudAccountDomainService;

    private Long tenantId;
    private String accountId;
    private ProviderType providerType;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        accountId = "123456789012";
        providerType = ProviderType.AWS;
    }

    @Nested
    @DisplayName("계정 중복 검증 테스트")
    class ValidateAccountUniquenessTest {

        @Test
        @DisplayName("중복되지 않은 계정인 경우 예외가 발생하지 않는다")
        void validateAccountUniqueness_NotDuplicate_Success() {
            // given
            given(cloudAccountRepository.existsByTenantIdAndAccountScope(tenantId, accountId))
                    .willReturn(false);
            
            // when & then - 예외가 발생하지 않아야 함
            cloudAccountDomainService.validateAccountUniqueness(tenantId, accountId, providerType);
            
            // verify
            then(cloudAccountRepository).should(times(1))
                    .existsByTenantIdAndAccountScope(tenantId, accountId);
        }

        @Test
        @DisplayName("중복된 계정인 경우 BusinessException을 던진다")
        void validateAccountUniqueness_Duplicate_ThrowsException() {
            // given
            given(cloudAccountRepository.existsByTenantIdAndAccountScope(tenantId, accountId))
                    .willReturn(true);
            
            // when & then
            assertThatThrownBy(() -> 
                    cloudAccountDomainService.validateAccountUniqueness(tenantId, accountId, providerType))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 등록된 계정입니다")
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.DUPLICATE_ACCOUNT);
            
            // verify
            then(cloudAccountRepository).should(times(1))
                    .existsByTenantIdAndAccountScope(tenantId, accountId);
        }

        @Test
        @DisplayName("accountScope가 null인 경우 검증을 수행하지 않는다")
        void validateAccountUniqueness_NullAccountScope_SkipsValidation() {
            // given
            String nullAccountScope = null;
            
            // when & then - 예외가 발생하지 않아야 함
            cloudAccountDomainService.validateAccountUniqueness(tenantId, nullAccountScope, providerType);
            
            // verify - Repository 호출이 없어야 함
            then(cloudAccountRepository).should(never())
                    .existsByTenantIdAndAccountScope(any(), any());
        }

        @Test
        @DisplayName("accountScope가 빈 문자열인 경우 검증을 수행하지 않는다")
        void validateAccountUniqueness_EmptyAccountScope_SkipsValidation() {
            // given
            String emptyAccountScope = "   ";
            
            // when & then - 예외가 발생하지 않아야 함
            cloudAccountDomainService.validateAccountUniqueness(tenantId, emptyAccountScope, providerType);
            
            // verify - Repository 호출이 없어야 함
            then(cloudAccountRepository).should(never())
                    .existsByTenantIdAndAccountScope(any(), any());
        }
    }

    @Nested
    @DisplayName("기본 계정 설정 테스트")
    class HandleDefaultAccountSettingTest {

        @Test
        @DisplayName("기존 기본 계정이 없는 경우 아무 작업도 수행하지 않는다")
        void handleDefaultAccountSetting_NoExistingDefault_DoesNothing() {
            // given
            given(cloudAccountRepository.findDefaultAccountsByTenantAndProviderType(tenantId, providerType))
                    .willReturn(List.of());

            // when
            cloudAccountDomainService.handleDefaultAccountSetting(tenantId, providerType);

            // then
            then(cloudAccountRepository).should(times(1))
                    .findDefaultAccountsByTenantAndProviderType(tenantId, providerType);
            then(cloudAccountRepository).should(never()).save(any(CloudAccount.class));
        }

        @Test
        @DisplayName("기존 기본 계정이 있는 경우 기본 설정을 해제한다")
        void handleDefaultAccountSetting_ExistingDefault_UnsetsDefault() {
            // given
            CloudProvider testProvider = CloudProvider.builder()
                    .providerType(providerType)
                    .providerName("AWS")
                    .build();
            testProvider.setId(1L);
            
            CloudAccount existingDefaultAccount = CloudAccount.builder()
                    .accountName("Existing Default Account")
                    .isDefault(true)
                    .provider(testProvider)
                    .build();
            existingDefaultAccount.setId(1L);

            given(cloudAccountRepository.findDefaultAccountsByTenantAndProviderType(tenantId, providerType))
                    .willReturn(List.of(existingDefaultAccount));
            given(cloudAccountRepository.save(any(CloudAccount.class)))
                    .willReturn(existingDefaultAccount);

            // when
            cloudAccountDomainService.handleDefaultAccountSetting(tenantId, providerType);

            // then
            assertThat(existingDefaultAccount.getIsDefault()).isFalse();
            then(cloudAccountRepository).should(times(1)).save(existingDefaultAccount);
        }

        @Test
        @DisplayName("기존 기본 계정이 여러 개인 경우 모두 기본 설정을 해제한다")
        void handleDefaultAccountSetting_MultipleExistingDefaults_UnsetsAll() {
            // given
            CloudProvider testProvider = CloudProvider.builder()
                    .providerType(providerType)
                    .providerName("AWS")
                    .build();
            testProvider.setId(1L);
            
            CloudAccount defaultAccount1 = CloudAccount.builder()
                    .accountName("Default Account 1")
                    .isDefault(true)
                    .provider(testProvider)
                    .build();
            defaultAccount1.setId(1L);

            CloudAccount defaultAccount2 = CloudAccount.builder()
                    .accountName("Default Account 2")
                    .isDefault(true)
                    .provider(testProvider)
                    .build();
            defaultAccount2.setId(2L);

            given(cloudAccountRepository.findDefaultAccountsByTenantAndProviderType(tenantId, providerType))
                    .willReturn(Arrays.asList(defaultAccount1, defaultAccount2));

            // when
            cloudAccountDomainService.handleDefaultAccountSetting(tenantId, providerType);

            // then
            assertThat(defaultAccount1.getIsDefault()).isFalse();
            assertThat(defaultAccount2.getIsDefault()).isFalse();
            then(cloudAccountRepository).should(times(2)).save(any(CloudAccount.class));
        }
    }

    @Nested
    @DisplayName("계정 상태 변경 테스트")
    class ChangeAccountStatusTest {

        private CloudAccount account;

        @BeforeEach
        void setUp() {
            account = CloudAccount.builder()
                    .accountName("Test Account")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            account.setId(1L);
        }

        @Test
        @DisplayName("ACTIVE 상태로 변경 성공")
        void changeAccountStatus_ToActive_Success() {
            // given
            account.setAccountStatus(AccountStatus.INACTIVE);
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(account);

            // when
            cloudAccountDomainService.changeAccountStatus(account, AccountStatus.ACTIVE);

            // then
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            then(cloudAccountRepository).should(times(1)).save(account);
        }

        @Test
        @DisplayName("INACTIVE 상태로 변경 성공")
        void changeAccountStatus_ToInactive_Success() {
            // given
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(account);

            // when
            cloudAccountDomainService.changeAccountStatus(account, AccountStatus.INACTIVE);

            // then
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
            then(cloudAccountRepository).should(times(1)).save(account);
        }

        @Test
        @DisplayName("SUSPENDED 상태로 변경 성공")
        void changeAccountStatus_ToSuspended_Success() {
            // given
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(account);

            // when
            cloudAccountDomainService.changeAccountStatus(account, AccountStatus.SUSPENDED);

            // then
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
            then(cloudAccountRepository).should(times(1)).save(account);
        }

        @Test
        @DisplayName("VERIFIED 상태로 변경 성공 - verifiedAt이 설정된다")
        void changeAccountStatus_ToVerified_Success() {
            // given
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(account);

            // when
            cloudAccountDomainService.changeAccountStatus(account, AccountStatus.VERIFIED);

            // then
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.VERIFIED);
            assertThat(account.getVerifiedAt()).isNotNull();
            then(cloudAccountRepository).should(times(1)).save(account);
        }

        @Test
        @DisplayName("FAILED 상태로 변경 성공")
        void changeAccountStatus_ToFailed_Success() {
            // given
            given(cloudAccountRepository.save(any(CloudAccount.class))).willReturn(account);

            // when
            cloudAccountDomainService.changeAccountStatus(account, AccountStatus.FAILED);

            // then
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.FAILED);
            then(cloudAccountRepository).should(times(1)).save(account);
        }

        @Test
        @DisplayName("VERIFYING 상태로 직접 변경 시도 시 예외 발생")
        void changeAccountStatus_ToVerifying_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> 
                    cloudAccountDomainService.changeAccountStatus(account, AccountStatus.VERIFYING))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("VERIFYING 상태로 직접 변경할 수 없습니다")
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.INVALID_ACCOUNT_STATUS);

            // verify
            then(cloudAccountRepository).should(never()).save(any(CloudAccount.class));
        }
    }

    @Nested
    @DisplayName("계정 삭제 검증 테스트")
    class ValidateAccountDeletionTest {

        @Test
        @DisplayName("삭제 가능한 계정인 경우 예외가 발생하지 않는다")
        void validateAccountDeletion_ValidAccount_Success() {
            // given
            CloudAccount account = CloudAccount.builder()
                    .accountName("Test Account")
                    .build();
            account.setId(1L);

            // when & then - 예외가 발생하지 않아야 함
            cloudAccountDomainService.validateAccountDeletion(account);
        }

        // TODO: 향후 연결된 리소스 검증 로직 추가 시 추가 테스트 작성
    }
}

