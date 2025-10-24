package com.agenticcp.core.domain.cloud.adapter.router;

import com.agenticcp.core.domain.cloud.adapter.aws.AwsAccountValidationAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CloudAccountValidationAdapterRouter 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudAccountValidationAdapterRouter 테스트")
class CloudAccountValidationAdapterRouterTest {
    
    @Mock
    private AwsAccountValidationAdapter awsAdapter;
    
    @InjectMocks
    private CloudAccountValidationAdapterRouter router;
    
    private AccountValidationRequest awsRequest;
    private AccountValidationRequest gcpRequest;
    private AccountValidationRequest azureRequest;
    
    @BeforeEach
    void setUp() {
        awsRequest = AccountValidationRequest.builder()
                .providerType(ProviderType.AWS)
                .accountId("123456789012")
                .region("us-east-1")
                .build();
        
        gcpRequest = AccountValidationRequest.builder()
                .providerType(ProviderType.GCP)
                .accountId("my-gcp-project")
                .region("us-central1")
                .build();
        
        azureRequest = AccountValidationRequest.builder()
                .providerType(ProviderType.AZURE)
                .accountId("12345678-1234-1234-1234-123456789012")
                .region("eastus")
                .build();
    }
    
    @Nested
    @DisplayName("AWS 어댑터 라우팅 테스트")
    class AwsAdapterRoutingTest {
        
        @Test
        @DisplayName("AWS 계정 검증 성공")
        void validateAccount_AwsProvider_Success() {
            // given
            AccountValidationResult expectedResult = AccountValidationResult.success(
                    "123456789012", 
                    ProviderType.AWS, 
                    AccountMetadata.builder()
                            .providerType(ProviderType.AWS)
                            .accountId("123456789012")
                            .build()
            );
            
            when(awsAdapter.validateAccount(any(AccountValidationRequest.class))).thenReturn(expectedResult);
            
            // when
            AccountValidationResult result = router.validateAccount(awsRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getProviderType()).isEqualTo(ProviderType.AWS);
            assertThat(result.getAccountId()).isEqualTo("123456789012");
        }
        
        @Test
        @DisplayName("AWS 계정 메타데이터 조회 성공")
        void getAccountMetadata_AwsProvider_Success() {
            // given
            AccountMetadata expectedMetadata = AccountMetadata.builder()
                    .providerType(ProviderType.AWS)
                    .accountId("123456789012")
                    .accountName("AWS Account 123456789012")
                    .build();
            
            when(awsAdapter.getAccountMetadata(any(AccountValidationRequest.class))).thenReturn(expectedMetadata);
            
            // when
            AccountMetadata result = router.getAccountMetadata(awsRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getProviderType()).isEqualTo(ProviderType.AWS);
            assertThat(result.getAccountId()).isEqualTo("123456789012");
        }
        
        @Test
        @DisplayName("AWS 연결 테스트 성공")
        void testConnection_AwsProvider_Success() {
            // given
            AccountValidationResult expectedResult = AccountValidationResult.success(
                    "123456789012", 
                    ProviderType.AWS, 
                    null
            );
            
            when(awsAdapter.testConnection(any(AccountValidationRequest.class))).thenReturn(expectedResult);
            
            // when
            AccountValidationResult result = router.testConnection(awsRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getProviderType()).isEqualTo(ProviderType.AWS);
        }
    }
    
    @Nested
    @DisplayName("GCP 어댑터 라우팅 테스트")
    class GcpAdapterRoutingTest {
        
        @Test
        @DisplayName("GCP 계정 검증 - 미구현")
        void validateAccount_GcpProvider_NotImplemented() {
            // when
            AccountValidationResult result = router.validateAccount(gcpRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNSUPPORTED_PROVIDER");
            assertThat(result.getMessage()).contains("Unsupported cloud provider: GCP");
        }
        
        @Test
        @DisplayName("GCP 계정 메타데이터 조회 - 미구현")
        void getAccountMetadata_GcpProvider_NotImplemented() {
            // when
            AccountMetadata result = router.getAccountMetadata(gcpRequest);
            
            // then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("GCP 연결 테스트 - 미구현")
        void testConnection_GcpProvider_NotImplemented() {
            // when
            AccountValidationResult result = router.testConnection(gcpRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNSUPPORTED_PROVIDER");
            assertThat(result.getMessage()).contains("Unsupported cloud provider: GCP");
        }
    }
    
    @Nested
    @DisplayName("Azure 어댑터 라우팅 테스트")
    class AzureAdapterRoutingTest {
        
        @Test
        @DisplayName("Azure 계정 검증 - 미구현")
        void validateAccount_AzureProvider_NotImplemented() {
            // when
            AccountValidationResult result = router.validateAccount(azureRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNSUPPORTED_PROVIDER");
            assertThat(result.getMessage()).contains("Unsupported cloud provider: AZURE");
        }
        
        @Test
        @DisplayName("Azure 계정 메타데이터 조회 - 미구현")
        void getAccountMetadata_AzureProvider_NotImplemented() {
            // when
            AccountMetadata result = router.getAccountMetadata(azureRequest);
            
            // then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("Azure 연결 테스트 - 미구현")
        void testConnection_AzureProvider_NotImplemented() {
            // when
            AccountValidationResult result = router.testConnection(azureRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNSUPPORTED_PROVIDER");
            assertThat(result.getMessage()).contains("Unsupported cloud provider: AZURE");
        }
    }
    
    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {
        
        @Test
        @DisplayName("null 프로바이더 타입")
        void validateAccount_NullProviderType_Unsupported() {
            // given
            AccountValidationRequest nullRequest = AccountValidationRequest.builder()
                    .providerType(null)
                    .accountId("123456789012")
                    .build();
            
            // when
            AccountValidationResult result = router.validateAccount(nullRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNSUPPORTED_PROVIDER");
        }
        
        @Test
        @DisplayName("AWS 어댑터 예외 발생")
        void validateAccount_AwsAdapterException_Propagated() {
            // given
            RuntimeException exception = new RuntimeException("AWS adapter error");
            when(awsAdapter.validateAccount(any(AccountValidationRequest.class))).thenThrow(exception);
            
            // when & then
            try {
                router.validateAccount(awsRequest);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("AWS adapter error");
            }
        }
    }
}
