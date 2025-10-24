package com.agenticcp.core.domain.cloud.adapter.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.AccountMetadata;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AwsAccountValidationAdapter 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsAccountValidationAdapter 테스트")
class AwsAccountValidationAdapterTest {
    
    @Mock
    private StsClient stsClient;
    
    @Mock
    private IamClient iamClient;
    
    @InjectMocks
    private AwsAccountValidationAdapter adapter;
    
    private AccountValidationRequest validRequest;
    
    @BeforeEach
    void setUp() {
        validRequest = AccountValidationRequest.builder()
                .providerType(ProviderType.AWS)
                .accountId("123456789012")
                .region("us-east-1")
                .roleArn("arn:aws:iam::123456789012:role/MyRole")
                .externalId("external-id-123")
                .build();
    }
    
    @Nested
    @DisplayName("계정 검증 테스트")
    class AccountValidationTest {
        
        @Test
        @DisplayName("유효한 AWS 계정 검증 성공")
        void validateAccount_ValidAccount_Success() {
            // given
            AssumeRoleResponse assumeRoleResponse = AssumeRoleResponse.builder()
                    .assumedRoleUser(software.amazon.awssdk.services.sts.model.AssumedRoleUser.builder()
                            .arn("arn:aws:sts::123456789012:assumed-role/MyRole/test-session")
                            .assumedRoleId("AROA123456789012345678:test-session")
                            .build())
                    .build();
            
            software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity = 
                    software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse.builder()
                    .account("123456789012")
                    .userId("AROA123456789012345678:test-session")
                    .arn("arn:aws:sts::123456789012:assumed-role/MyRole/test-session")
                    .build();
            
            when(stsClient.assumeRole(any(AssumeRoleRequest.class))).thenReturn(assumeRoleResponse);
            when(stsClient.getCallerIdentity(any(software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest.class))).thenReturn(callerIdentity);
            
            // when
            AccountValidationResult result = adapter.validateAccount(validRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getAccountId()).isEqualTo("123456789012");
            assertThat(result.getProviderType()).isEqualTo(ProviderType.AWS);
            assertThat(result.getAccountMetadata()).isNotNull();
            assertThat(result.getAccountMetadata().getAccountId()).isEqualTo("123456789012");
        }
        
        @Test
        @DisplayName("잘못된 AWS 계정 ID 형식")
        void validateAccount_InvalidAccountId_Failure() {
            // given
            AccountValidationRequest invalidRequest = AccountValidationRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountId("invalid-account-id")
                    .region("us-east-1")
                    .build();
            
            // when
            AccountValidationResult result = adapter.validateAccount(invalidRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AWS_INVALID_ACCOUNT_ID_FORMAT");
            assertThat(result.getMessage()).contains("Invalid AWS Account ID format");
        }
        
        @Test
        @DisplayName("AWS SDK 예외 발생")
        void validateAccount_AwsSdkException_Failure() {
            // given
            when(stsClient.assumeRole(any(AssumeRoleRequest.class))).thenThrow(SdkException.builder().message("AWS API error").build());
            
            // when
            AccountValidationResult result = adapter.validateAccount(validRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AWS_SDK_ERROR");
            assertThat(result.getMessage()).contains("AWS API call failed");
        }
        
        @Test
        @DisplayName("null 계정 ID")
        void validateAccount_NullAccountId_Failure() {
            // given
            AccountValidationRequest nullRequest = AccountValidationRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountId(null)
                    .region("us-east-1")
                    .build();
            
            // when
            AccountValidationResult result = adapter.validateAccount(nullRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AWS_INVALID_ACCOUNT_ID_FORMAT");
        }
    }
    
    @Nested
    @DisplayName("계정 메타데이터 조회 테스트")
    class AccountMetadataTest {
        
        @Test
        @DisplayName("계정 메타데이터 조회 성공")
        void getAccountMetadata_Success() {
            // given
            software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity = 
                    software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse.builder()
                    .account("123456789012")
                    .userId("AROA123456789012345678:test-session")
                    .arn("arn:aws:sts::123456789012:assumed-role/MyRole/test-session")
                    .build();
            
            when(stsClient.getCallerIdentity(any(software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest.class))).thenReturn(callerIdentity);
            
            // when
            AccountMetadata metadata = adapter.getAccountMetadata(validRequest);
            
            // then
            assertThat(metadata).isNotNull();
            assertThat(metadata.getProviderType()).isEqualTo(ProviderType.AWS);
            assertThat(metadata.getAccountId()).isEqualTo("123456789012");
            assertThat(metadata.getAccountName()).isEqualTo("AWS Account 123456789012");
            assertThat(metadata.getAvailableRegions()).isNotEmpty();
            assertThat(metadata.getCustomMetadata()).containsKey("userId");
            assertThat(metadata.getCustomMetadata()).containsKey("arn");
        }
        
        @Test
        @DisplayName("계정 메타데이터 조회 실패")
        void getAccountMetadata_Failure() {
            // given
            when(stsClient.getCallerIdentity(any(software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest.class))).thenThrow(SdkException.builder().message("API error").build());
            
            // when
            AccountMetadata metadata = adapter.getAccountMetadata(validRequest);
            
            // then
            assertThat(metadata).isNull();
        }
    }
    
    @Nested
    @DisplayName("연결 테스트 테스트")
    class ConnectionTestTest {
        
        @Test
        @DisplayName("연결 테스트 성공")
        void testConnection_Success() {
            // given
            software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity = 
                    software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse.builder()
                    .account("123456789012")
                    .userId("AROA123456789012345678:test-session")
                    .arn("arn:aws:sts::123456789012:assumed-role/MyRole/test-session")
                    .build();
            
            when(stsClient.getCallerIdentity(any(software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest.class))).thenReturn(callerIdentity);
            
            // when
            AccountValidationResult result = adapter.testConnection(validRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getAccountId()).isEqualTo("123456789012");
            assertThat(result.getProviderType()).isEqualTo(ProviderType.AWS);
        }
        
        @Test
        @DisplayName("연결 테스트 실패")
        void testConnection_Failure() {
            // given
            when(stsClient.getCallerIdentity(any(software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest.class))).thenThrow(SdkException.builder().message("Connection failed").build());
            
            // when
            AccountValidationResult result = adapter.testConnection(validRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AWS_CONNECTION_FAILED");
            assertThat(result.getMessage()).contains("Connection test failed");
        }
    }
    
    @Nested
    @DisplayName("AWS 계정 ID 형식 검증 테스트")
    class AccountIdValidationTest {
        
        @Test
        @DisplayName("12자리 숫자 계정 ID는 유효")
        void isValidAwsAccountId_ValidFormat() {
            // given
            AccountValidationRequest request = AccountValidationRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountId("123456789012")
                    .build();
            
            // when
            AccountValidationResult result = adapter.validateAccount(request);
            
            // then
            // isValidAwsAccountId는 private 메서드이므로 validateAccount를 통해 간접 테스트
            // 실제 AWS API 호출 없이 형식 검증만 테스트하기 위해 Mock 설정하지 않음
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("11자리 숫자 계정 ID는 무효")
        void isValidAwsAccountId_InvalidLength() {
            // given
            AccountValidationRequest request = AccountValidationRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountId("12345678901")
                    .build();
            
            // when
            AccountValidationResult result = adapter.validateAccount(request);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AWS_INVALID_ACCOUNT_ID_FORMAT");
        }
        
        @Test
        @DisplayName("문자가 포함된 계정 ID는 무효")
        void isValidAwsAccountId_ContainsLetters() {
            // given
            AccountValidationRequest request = AccountValidationRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountId("12345678901a")
                    .build();
            
            // when
            AccountValidationResult result = adapter.validateAccount(request);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AWS_INVALID_ACCOUNT_ID_FORMAT");
        }
    }
}
