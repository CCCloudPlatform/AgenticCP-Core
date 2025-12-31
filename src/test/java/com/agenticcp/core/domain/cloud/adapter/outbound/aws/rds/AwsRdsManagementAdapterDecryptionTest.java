package com.agenticcp.core.domain.cloud.adapter.outbound.aws.rds;

import com.agenticcp.core.common.crypto.AesGcmEncryptionService;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsRdsConfig;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsUpdateCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AwsRdsManagementAdapter 복호화 로직 단위 테스트
 * 
 * adminPassword 복호화 로직을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsRdsManagementAdapter 복호화 테스트")
class AwsRdsManagementAdapterDecryptionTest {

    @Mock
    private AwsRdsConfig awsRdsConfig;

    @Mock
    private AwsRdsMapper mapper;

    @Mock
    private RdsClient rdsClient;

    @Mock
    private CloudSessionCredential session;

    private EncryptionService encryptionService;
    private AwsRdsManagementAdapter adapter;

    @BeforeEach
    void setUp() {
        // 실제 EncryptionService 사용 (AES-GCM-256)
        byte[] key = generateKey(32);
        encryptionService = new AesGcmEncryptionService(key);

        // AwsRdsManagementAdapter 인스턴스 생성 (EncryptionService 주입)
        adapter = new AwsRdsManagementAdapter(awsRdsConfig, mapper, encryptionService);

        // Mock 설정 - lenient()를 사용하여 각 테스트에서 다른 인자로 호출될 수 있음
        lenient().when(awsRdsConfig.createRdsClient(any(CloudSessionCredential.class), anyString()))
            .thenReturn(rdsClient);
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
    @DisplayName("adminPassword 복호화 테스트 - 생성 시")
    class CreateRdbmsDecryptionTest {

        @Test
        @DisplayName("암호화된 adminPassword가 정상적으로 복호화되어 AWS SDK 요청에 전달됨")
        void shouldDecryptPasswordWhenCreatingRdbms() throws Exception {
            // given
            String plainPassword = "MySecurePassword123!";
            String encryptedPassword = encryptionService.encrypt(plainPassword);

            RdbmsCreateCommand command = RdbmsCreateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .serviceKey("RDS")
                .resourceType("DATABASE")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .adminUsername("admin")
                .adminPassword(encryptedPassword)  // 암호화된 패스워드
                .session(session)  // session 추가
                .build();

            // AWS SDK 응답 Mock
            DBInstance dbInstance = DBInstance.builder()
                .dbInstanceIdentifier("test-db")
                .dbInstanceStatus("available")
                .build();

            CreateDbInstanceResponse response = CreateDbInstanceResponse.builder()
                .dbInstance(dbInstance)
                .build();

            DescribeDbInstancesResponse describeResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(dbInstance)
                .build();

            given(rdsClient.createDBInstance(any(CreateDbInstanceRequest.class)))
                .willReturn(response);
            given(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .willReturn(describeResponse);
            given(mapper.toCloudResource(any(DBInstance.class), any(), any(), any()))
                .willReturn(mock(com.agenticcp.core.domain.cloud.entity.CloudResource.class));

            ArgumentCaptor<CreateDbInstanceRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateDbInstanceRequest.class);

            // when
            adapter.createRdbms(command);

            // then
            verify(rdsClient).createDBInstance(requestCaptor.capture());
            CreateDbInstanceRequest capturedRequest = requestCaptor.getValue();
            
            // AWS SDK 요청에 전달된 패스워드가 복호화된 평문인지 확인
            String requestPassword = capturedRequest.masterUserPassword();
            assertThat(requestPassword).isNotNull();
            assertThat(requestPassword).isEqualTo(plainPassword);
            assertThat(requestPassword).isNotEqualTo(encryptedPassword);
        }

        @Test
        @DisplayName("복호화 실패 시 DECRYPTION_FAILED 예외 발생")
        void shouldThrowExceptionWhenDecryptionFails() {
            // given
            EncryptionService failingEncryptionService = mock(EncryptionService.class);
            AwsRdsManagementAdapter adapterWithFailingDecryption = 
                new AwsRdsManagementAdapter(awsRdsConfig, mapper, failingEncryptionService);

            String invalidEncryptedPassword = "invalid-encrypted-password";
            RdbmsCreateCommand command = RdbmsCreateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .serviceKey("RDS")
                .resourceType("DATABASE")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .adminUsername("admin")
                .adminPassword(invalidEncryptedPassword)
                .session(session)  // session 추가
                .build();

            given(awsRdsConfig.createRdsClient(any(CloudSessionCredential.class), anyString()))
                .willReturn(rdsClient);
            given(failingEncryptionService.decrypt(anyString()))
                .willThrow(new RuntimeException("복호화 실패"));

            // when & then
            assertThatThrownBy(() -> adapterWithFailingDecryption.createRdbms(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.DECRYPTION_FAILED);
                    assertThat(be.getMessage()).contains("관리자 패스워드 복호화에 실패했습니다");
                });
        }

        @Test
        @DisplayName("adminPassword가 null이면 AWS SDK 요청에도 null이 전달됨")
        void shouldPassNullWhenPasswordIsNull() throws Exception {
            // given
            RdbmsCreateCommand command = RdbmsCreateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .serviceKey("RDS")
                .resourceType("DATABASE")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .adminUsername("admin")
                .adminPassword(null)  // null 패스워드
                .session(session)  // session 추가
                .build();

            // AWS SDK 응답 Mock
            DBInstance dbInstance = DBInstance.builder()
                .dbInstanceIdentifier("test-db")
                .dbInstanceStatus("available")
                .build();

            CreateDbInstanceResponse response = CreateDbInstanceResponse.builder()
                .dbInstance(dbInstance)
                .build();

            DescribeDbInstancesResponse describeResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(dbInstance)
                .build();

            given(rdsClient.createDBInstance(any(CreateDbInstanceRequest.class)))
                .willReturn(response);
            given(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .willReturn(describeResponse);
            given(mapper.toCloudResource(any(DBInstance.class), any(), any(), any()))
                .willReturn(mock(com.agenticcp.core.domain.cloud.entity.CloudResource.class));

            ArgumentCaptor<CreateDbInstanceRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateDbInstanceRequest.class);

            // when
            adapter.createRdbms(command);

            // then
            verify(rdsClient).createDBInstance(requestCaptor.capture());
            CreateDbInstanceRequest capturedRequest = requestCaptor.getValue();
            
            // AWS SDK 요청에 전달된 패스워드가 null인지 확인
            String requestPassword = capturedRequest.masterUserPassword();
            assertThat(requestPassword).isNull();
        }
    }

    @Nested
    @DisplayName("adminPassword 복호화 테스트 - 수정 시")
    class UpdateRdbmsDecryptionTest {

        @Test
        @DisplayName("암호화된 adminPassword가 정상적으로 복호화되어 AWS SDK 요청에 전달됨")
        void shouldDecryptPasswordWhenUpdatingRdbms() throws Exception {
            // given
            String plainPassword = "NewSecurePassword456!";
            String encryptedPassword = encryptionService.encrypt(plainPassword);

            RdbmsUpdateCommand command = RdbmsUpdateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .providerResourceId("db-instance-123")
                .adminPassword(encryptedPassword)  // 암호화된 패스워드
                .session(session)  // session 추가
                .build();

            // AWS SDK 응답 Mock
            DBInstance dbInstance = DBInstance.builder()
                .dbInstanceIdentifier("db-instance-123")
                .dbInstanceStatus("available")
                .build();

            ModifyDbInstanceResponse response = ModifyDbInstanceResponse.builder()
                .dbInstance(dbInstance)
                .build();

            DescribeDbInstancesResponse describeResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(dbInstance)
                .build();

            given(rdsClient.modifyDBInstance(any(ModifyDbInstanceRequest.class)))
                .willReturn(response);
            given(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .willReturn(describeResponse);
            given(mapper.toCloudResource(any(DBInstance.class), any(), any(), any()))
                .willReturn(mock(com.agenticcp.core.domain.cloud.entity.CloudResource.class));

            ArgumentCaptor<ModifyDbInstanceRequest> requestCaptor = 
                ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);

            // when
            adapter.updateRdbms(command);

            // then
            verify(rdsClient).modifyDBInstance(requestCaptor.capture());
            ModifyDbInstanceRequest capturedRequest = requestCaptor.getValue();
            
            // AWS SDK 요청에 전달된 패스워드가 복호화된 평문인지 확인
            String requestPassword = capturedRequest.masterUserPassword();
            assertThat(requestPassword).isNotNull();
            assertThat(requestPassword).isEqualTo(plainPassword);
            assertThat(requestPassword).isNotEqualTo(encryptedPassword);
        }

        @Test
        @DisplayName("복호화 실패 시 DECRYPTION_FAILED 예외 발생")
        void shouldThrowExceptionWhenDecryptionFails() {
            // given
            EncryptionService failingEncryptionService = mock(EncryptionService.class);
            AwsRdsManagementAdapter adapterWithFailingDecryption = 
                new AwsRdsManagementAdapter(awsRdsConfig, mapper, failingEncryptionService);

            String invalidEncryptedPassword = "invalid-encrypted-password";
            RdbmsUpdateCommand command = RdbmsUpdateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .providerResourceId("db-instance-123")
                .adminPassword(invalidEncryptedPassword)
                .session(session)  // session 추가
                .build();

            given(awsRdsConfig.createRdsClient(any(CloudSessionCredential.class), anyString()))
                .willReturn(rdsClient);
            given(failingEncryptionService.decrypt(anyString()))
                .willThrow(new RuntimeException("복호화 실패"));

            // when & then
            assertThatThrownBy(() -> adapterWithFailingDecryption.updateRdbms(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.DECRYPTION_FAILED);
                    assertThat(be.getMessage()).contains("관리자 패스워드 복호화에 실패했습니다");
                });
        }

        @Test
        @DisplayName("adminPassword가 null이면 AWS SDK 요청에도 null이 전달됨")
        void shouldPassNullWhenPasswordIsNull() throws Exception {
            // given
            RdbmsUpdateCommand command = RdbmsUpdateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .providerResourceId("db-instance-123")
                .instanceSize("db.t3.small")
                .adminPassword(null)  // null 패스워드
                .session(session)  // session 추가
                .build();

            // AWS SDK 응답 Mock
            DBInstance dbInstance = DBInstance.builder()
                .dbInstanceIdentifier("db-instance-123")
                .dbInstanceStatus("available")
                .build();

            ModifyDbInstanceResponse response = ModifyDbInstanceResponse.builder()
                .dbInstance(dbInstance)
                .build();

            DescribeDbInstancesResponse describeResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(dbInstance)
                .build();

            given(rdsClient.modifyDBInstance(any(ModifyDbInstanceRequest.class)))
                .willReturn(response);
            given(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .willReturn(describeResponse);
            given(mapper.toCloudResource(any(DBInstance.class), any(), any(), any()))
                .willReturn(mock(com.agenticcp.core.domain.cloud.entity.CloudResource.class));

            ArgumentCaptor<ModifyDbInstanceRequest> requestCaptor = 
                ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);

            // when
            adapter.updateRdbms(command);

            // then
            verify(rdsClient).modifyDBInstance(requestCaptor.capture());
            ModifyDbInstanceRequest capturedRequest = requestCaptor.getValue();
            
            // AWS SDK 요청에 전달된 패스워드가 null인지 확인
            String requestPassword = capturedRequest.masterUserPassword();
            assertThat(requestPassword).isNull();
        }
    }

    @Nested
    @DisplayName("복호화 라운드트립 테스트")
    class DecryptionRoundTripTest {

        @Test
        @DisplayName("암호화된 패스워드를 복호화하면 원본과 일치함")
        void shouldDecryptToOriginalPassword() throws Exception {
            // given
            String plainPassword = "TestPassword123!@#";
            String encryptedPassword = encryptionService.encrypt(plainPassword);

            RdbmsCreateCommand command = RdbmsCreateCommand.builder()
                .providerType(CloudProvider.ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .serviceKey("RDS")
                .resourceType("DATABASE")
                .instanceName("test-db")
                .engine("mysql")
                .instanceSize("db.t3.micro")
                .allocatedStorage(20)
                .adminUsername("admin")
                .adminPassword(encryptedPassword)
                .session(session)  // session 추가
                .build();

            // AWS SDK 응답 Mock
            DBInstance dbInstance = DBInstance.builder()
                .dbInstanceIdentifier("test-db")
                .dbInstanceStatus("available")
                .build();

            CreateDbInstanceResponse response = CreateDbInstanceResponse.builder()
                .dbInstance(dbInstance)
                .build();

            DescribeDbInstancesResponse describeResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(dbInstance)
                .build();

            given(rdsClient.createDBInstance(any(CreateDbInstanceRequest.class)))
                .willReturn(response);
            given(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .willReturn(describeResponse);
            given(mapper.toCloudResource(any(DBInstance.class), any(), any(), any()))
                .willReturn(mock(com.agenticcp.core.domain.cloud.entity.CloudResource.class));

            ArgumentCaptor<CreateDbInstanceRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateDbInstanceRequest.class);

            // when
            adapter.createRdbms(command);

            // then
            verify(rdsClient).createDBInstance(requestCaptor.capture());
            CreateDbInstanceRequest capturedRequest = requestCaptor.getValue();
            
            String decryptedPassword = capturedRequest.masterUserPassword();
            assertThat(decryptedPassword).isEqualTo(plainPassword);
        }

        @Test
        @DisplayName("다양한 특수문자를 포함한 패스워드도 정상 복호화됨")
        void shouldHandleSpecialCharacters() throws Exception {
            // given
            String[] testPasswords = {
                "Password123!@#$%^&*()",
                "한글패스워드123!",
                "P@ssw0rd with spaces",
                "VeryLongPassword1234567890!@#$%^&*()_+-=[]{}|;:,.<>?",
                "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
            };

            for (String plainPassword : testPasswords) {
                String encryptedPassword = encryptionService.encrypt(plainPassword);

                RdbmsCreateCommand command = RdbmsCreateCommand.builder()
                    .providerType(CloudProvider.ProviderType.AWS)
                    .accountScope("123456789012")
                    .region("us-east-1")
                    .serviceKey("RDS")
                    .resourceType("DATABASE")
                    .instanceName("test-db")
                    .engine("mysql")
                    .instanceSize("db.t3.micro")
                    .allocatedStorage(20)
                    .adminUsername("admin")
                    .adminPassword(encryptedPassword)
                    .session(session)  // session 추가
                    .build();

                // AWS SDK 응답 Mock
                DBInstance dbInstance = DBInstance.builder()
                    .dbInstanceIdentifier("test-db")
                    .dbInstanceStatus("available")
                    .build();

                CreateDbInstanceResponse response = CreateDbInstanceResponse.builder()
                    .dbInstance(dbInstance)
                    .build();

                DescribeDbInstancesResponse describeResponse = DescribeDbInstancesResponse.builder()
                    .dbInstances(dbInstance)
                    .build();

                given(rdsClient.createDBInstance(any(CreateDbInstanceRequest.class)))
                    .willReturn(response);
                given(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                    .willReturn(describeResponse);
                given(mapper.toCloudResource(any(DBInstance.class), any(), any(), any()))
                    .willReturn(mock(com.agenticcp.core.domain.cloud.entity.CloudResource.class));

                ArgumentCaptor<CreateDbInstanceRequest> requestCaptor = 
                    ArgumentCaptor.forClass(CreateDbInstanceRequest.class);

                // when
                adapter.createRdbms(command);

                // then
                verify(rdsClient, atLeastOnce()).createDBInstance(requestCaptor.capture());
                CreateDbInstanceRequest capturedRequest = requestCaptor.getValue();
                
                String decryptedPassword = capturedRequest.masterUserPassword();
                assertThat(decryptedPassword).isEqualTo(plainPassword);
            }
        }
    }
}
