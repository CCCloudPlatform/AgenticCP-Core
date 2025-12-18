package com.agenticcp.core.domain.cloud.service.rdbms;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.crypto.AesGcmEncryptionService;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.RdbmsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * RdbmsUseCaseService 암호화/복호화 로직 단위 테스트
 * 
 * adminPassword 암호화 로직을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RdbmsUseCaseService 암호화/복호화 테스트")
class RdbmsUseCaseServiceEncryptionTest {

    @Mock
    private RdbmsPortRouter portRouter;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    @Mock
    private RdbmsManagementPort managementPort;

    private EncryptionService encryptionService;
    private RdbmsUseCaseService rdbmsUseCaseService;

    private String tenantKey;
    private CloudSessionCredential session;
    private CloudResource mockResource;

    @BeforeEach
    void setUp() {
        tenantKey = "test-tenant";
        TenantContextHolder.setTenantKey(tenantKey);

        // 실제 EncryptionService 사용 (AES-GCM-256)
        byte[] key = generateKey(32);
        encryptionService = new AesGcmEncryptionService(key);

        // RdbmsUseCaseService 인스턴스 생성 (EncryptionService 주입)
        rdbmsUseCaseService = new RdbmsUseCaseService(
            portRouter,
            capabilityGuard,
            credentialPort,
            resourceHelper,
            encryptionService
        );

        // Mock 설정
        session = mock(CloudSessionCredential.class);
        mockResource = mock(CloudResource.class);
        
        // lenient()를 사용하여 일부 테스트에서 사용되지 않을 수 있는 stubbing 허용
        lenient().when(credentialPort.getSession(eq(tenantKey), anyString(), any(CloudProvider.ProviderType.class)))
            .thenReturn(session);
        lenient().when(portRouter.management(any(CloudProvider.ProviderType.class)))
            .thenReturn(managementPort);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * AES 키 생성 헬퍼 메서드
     */
    private byte[] generateKey(int size) {
        byte[] key = new byte[size];
        new SecureRandom().nextBytes(key);
        return key;
    }

    @Nested
    @DisplayName("adminPassword 암호화 테스트 - 생성 시")
    class CreateRdbmsEncryptionTest {

        @Test
        @DisplayName("adminPassword가 정상적으로 암호화되어 Command에 전달됨")
        void shouldEncryptPasswordWhenCreatingRdbms() {
            // given
            String plainPassword = "MySecurePassword123!";
            RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .masterUsername("admin")
                .masterPassword(plainPassword)
                .build();

            doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
            given(managementPort.createRdbms(any(RdbmsCreateCommand.class)))
                .willReturn(mockResource);

            // when
            rdbmsUseCaseService.createRdbms(request);

            // then
            verify(managementPort).createRdbms(argThat(command -> {
                // Command에 전달된 adminPassword가 암호화되었는지 확인
                String encryptedPassword = command.adminPassword();
                assertThat(encryptedPassword).isNotNull();
                assertThat(encryptedPassword).isNotEqualTo(plainPassword);
                
                // 암호화된 값이 Base64 형식인지 확인
                assertThat(encryptedPassword).doesNotContain(plainPassword);
                
                // 복호화하여 원본과 일치하는지 확인
                String decrypted = encryptionService.decrypt(encryptedPassword);
                assertThat(decrypted).isEqualTo(plainPassword);
                
                return true;
            }));
        }

        @Test
        @DisplayName("암호화 실패 시 ENCRYPTION_FAILED 예외 발생")
        void shouldThrowExceptionWhenEncryptionFails() {
            // given
            EncryptionService failingEncryptionService = mock(EncryptionService.class);
            RdbmsUseCaseService serviceWithFailingEncryption = new RdbmsUseCaseService(
                portRouter,
                capabilityGuard,
                credentialPort,
                resourceHelper,
                failingEncryptionService
            );

            RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .masterUsername("admin")
                .masterPassword("MySecurePassword123!")
                .build();

            doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
            given(failingEncryptionService.encrypt(anyString()))
                .willThrow(new RuntimeException("암호화 실패"));

            // when & then
            assertThatThrownBy(() -> serviceWithFailingEncryption.createRdbms(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.ENCRYPTION_FAILED);
                    assertThat(be.getMessage()).contains("관리자 패스워드 암호화에 실패했습니다");
                });
        }
    }

    @Nested
    @DisplayName("adminPassword 암호화 테스트 - 수정 시")
    class UpdateRdbmsEncryptionTest {

        @Test
        @DisplayName("adminPassword가 제공되면 암호화되어 Command에 전달됨")
        void shouldEncryptPasswordWhenUpdatingRdbmsWithPassword() {
            // given
            String plainPassword = "NewSecurePassword456!";
            RdbmsUpdateRequest request = RdbmsUpdateRequest.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .instanceId("db-instance-123")
                .masterPassword(plainPassword)
                .build();

            doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
            given(managementPort.updateRdbms(any(RdbmsUpdateCommand.class)))
                .willReturn(mockResource);

            // when
            rdbmsUseCaseService.updateRdbms(request);

            // then
            verify(managementPort).updateRdbms(argThat(command -> {
                // Command에 전달된 adminPassword가 암호화되었는지 확인
                String encryptedPassword = command.adminPassword();
                assertThat(encryptedPassword).isNotNull();
                assertThat(encryptedPassword).isNotEqualTo(plainPassword);
                
                // 복호화하여 원본과 일치하는지 확인
                String decrypted = encryptionService.decrypt(encryptedPassword);
                assertThat(decrypted).isEqualTo(plainPassword);
                
                return true;
            }));
        }

        @Test
        @DisplayName("adminPassword가 null이면 Command에 null이 전달됨")
        void shouldPassNullWhenPasswordNotProvided() {
            // given
            RdbmsUpdateRequest request = RdbmsUpdateRequest.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .instanceId("db-instance-123")
                .instanceSize("db.t3.small")
                .build();

            doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
            given(managementPort.updateRdbms(any(RdbmsUpdateCommand.class)))
                .willReturn(mockResource);

            // when
            rdbmsUseCaseService.updateRdbms(request);

            // then
            verify(managementPort).updateRdbms(argThat(command -> {
                // Command에 전달된 adminPassword가 null인지 확인
                assertThat(command.adminPassword()).isNull();
                return true;
            }));
        }

        @Test
        @DisplayName("암호화 실패 시 ENCRYPTION_FAILED 예외 발생")
        void shouldThrowExceptionWhenEncryptionFails() {
            // given
            EncryptionService failingEncryptionService = mock(EncryptionService.class);
            RdbmsUseCaseService serviceWithFailingEncryption = new RdbmsUseCaseService(
                portRouter,
                capabilityGuard,
                credentialPort,
                resourceHelper,
                failingEncryptionService
            );

            RdbmsUpdateRequest request = RdbmsUpdateRequest.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .instanceId("db-instance-123")
                .masterPassword("NewSecurePassword456!")
                .build();

            doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
            given(failingEncryptionService.encrypt(anyString()))
                .willThrow(new RuntimeException("암호화 실패"));

            // when & then
            assertThatThrownBy(() -> serviceWithFailingEncryption.updateRdbms(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.ENCRYPTION_FAILED);
                    assertThat(be.getMessage()).contains("관리자 패스워드 암호화에 실패했습니다");
                });
        }
    }

    @Nested
    @DisplayName("암호화 라운드트립 테스트")
    class EncryptionRoundTripTest {

        @Test
        @DisplayName("암호화 후 복호화하면 원본과 일치함")
        void shouldDecryptToOriginalPassword() {
            // given
            String plainPassword = "TestPassword123!@#";
            RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .masterUsername("admin")
                .masterPassword(plainPassword)
                .build();

            doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
            given(managementPort.createRdbms(any(RdbmsCreateCommand.class)))
                .willReturn(mockResource);

            // when
            rdbmsUseCaseService.createRdbms(request);

            // then
            verify(managementPort).createRdbms(argThat(command -> {
                String encryptedPassword = command.adminPassword();
                
                // 암호화된 값이 원본과 다름
                assertThat(encryptedPassword).isNotEqualTo(plainPassword);
                
                // 복호화하면 원본과 일치
                String decrypted = encryptionService.decrypt(encryptedPassword);
                assertThat(decrypted).isEqualTo(plainPassword);
                
                return true;
            }));
        }

        @Test
        @DisplayName("다양한 특수문자를 포함한 패스워드도 정상 암호화/복호화됨")
        void shouldHandleSpecialCharacters() {
            // given
            String[] testPasswords = {
                "Password123!@#$%^&*()",
                "한글패스워드123!",
                "P@ssw0rd with spaces",
                "VeryLongPassword1234567890!@#$%^&*()_+-=[]{}|;:,.<>?",
                "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
            };

            ArgumentCaptor<RdbmsCreateCommand> commandCaptor = 
                ArgumentCaptor.forClass(RdbmsCreateCommand.class);

            for (String plainPassword : testPasswords) {
                RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                    .providerType(CloudProvider.ProviderType.AWS)
                    .accountScope("123456789012")
                    .region("us-east-1")
                    .instanceName("test-db")
                    .engine("mysql")
                    .instanceSize("db.t3.micro")
                    .allocatedStorage(20)
                    .masterUsername("admin")
                    .masterPassword(plainPassword)
                    .build();

                doNothing().when(capabilityGuard).ensureSupported(any(), any(), any(), any());
                given(managementPort.createRdbms(any(RdbmsCreateCommand.class)))
                    .willReturn(mockResource);

                // when
                rdbmsUseCaseService.createRdbms(request);

                // then - 각 호출마다 검증
                verify(managementPort).createRdbms(commandCaptor.capture());
                RdbmsCreateCommand capturedCommand = commandCaptor.getValue();
                
                String encryptedPassword = capturedCommand.adminPassword();
                assertThat(encryptedPassword).isNotNull();
                assertThat(encryptedPassword).isNotEqualTo(plainPassword);
                
                // 복호화하여 원본과 일치하는지 확인
                String decrypted = encryptionService.decrypt(encryptedPassword);
                assertThat(decrypted).isEqualTo(plainPassword);
                
                // 다음 반복을 위해 reset
                reset(managementPort);
            }
        }
    }
}
