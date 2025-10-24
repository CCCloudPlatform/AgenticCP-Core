package com.agenticcp.core.domain.cloud.validation;

import com.agenticcp.core.domain.cloud.dto.RegisterAwsAccountRequest;
import com.agenticcp.core.domain.cloud.dto.RegisterAzureAccountRequest;
import com.agenticcp.core.domain.cloud.dto.RegisterGcpAccountRequest;
import com.agenticcp.core.domain.cloud.entity.CloudAccount.AuthMethod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cloud Account Request Validator 테스트
 * 
 * CSP별 필수 필드 검증을 테스트합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@DisplayName("Cloud Account Request Validator 테스트")
class CloudAccountRequestValidatorTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Nested
    @DisplayName("AWS 계정 검증")
    class AwsAccountValidation {
        
        @Test
        @DisplayName("IAM_ROLE 인증 방식에서 roleArn이 있으면 검증 통과")
        void validAwsAccount_WithRoleArn() {
            // given
            RegisterAwsAccountRequest request = RegisterAwsAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(1L)
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .authMethod(AuthMethod.IAM_ROLE)
                    .roleArn("arn:aws:iam::123456789012:role/MyRole")
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAwsAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).isEmpty();
        }
        
        @Test
        @DisplayName("IAM_ROLE 인증 방식에서 roleArn이 없으면 검증 실패")
        void invalidAwsAccount_MissingRoleArn() {
            // given
            RegisterAwsAccountRequest request = RegisterAwsAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(1L)
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .authMethod(AuthMethod.IAM_ROLE)
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAwsAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("roleArn은 필수입니다");
        }
        
        @Test
        @DisplayName("ACCESS_KEY 인증 방식에서 roleArn이 없어도 검증 통과")
        void validAwsAccount_AccessKeyWithoutRoleArn() {
            // given
            RegisterAwsAccountRequest request = RegisterAwsAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(1L)
                    .accountId("123456789012")
                    .accountName("aws-prod-account")
                    .authMethod(AuthMethod.ACCESS_KEY)
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAwsAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("GCP 계정 검증")
    class GcpAccountValidation {
        
        @Test
        @DisplayName("serviceAccountEmail이 있으면 검증 통과")
        void validGcpAccount_WithServiceAccountEmail() {
            // given
            RegisterGcpAccountRequest request = RegisterGcpAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(2L)
                    .accountId("my-gcp-project")
                    .accountName("gcp-prod-account")
                    .authMethod(AuthMethod.SERVICE_ACCOUNT)
                    .serviceAccountEmail("service-account@my-gcp-project.iam.gserviceaccount.com")
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterGcpAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).isEmpty();
        }
        
        @Test
        @DisplayName("serviceAccountEmail이 없으면 검증 실패")
        void invalidGcpAccount_MissingServiceAccountEmail() {
            // given
            RegisterGcpAccountRequest request = RegisterGcpAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(2L)
                    .accountId("my-gcp-project")
                    .accountName("gcp-prod-account")
                    .authMethod(AuthMethod.SERVICE_ACCOUNT)
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterGcpAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("serviceAccountEmail은 필수입니다");
        }
    }
    
    @Nested
    @DisplayName("Azure 계정 검증")
    class AzureAccountValidation {
        
        @Test
        @DisplayName("azureTenantId와 azureClientId가 있으면 검증 통과")
        void validAzureAccount_WithRequiredFields() {
            // given
            RegisterAzureAccountRequest request = RegisterAzureAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(3L)
                    .accountId("12345678-1234-1234-1234-123456789012")
                    .accountName("azure-prod-account")
                    .authMethod(AuthMethod.SERVICE_PRINCIPAL)
                    .azureTenantId("87654321-4321-4321-4321-210987654321")
                    .azureClientId("abcdef12-3456-7890-abcd-ef1234567890")
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAzureAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).isEmpty();
        }
        
        @Test
        @DisplayName("azureTenantId가 없으면 검증 실패")
        void invalidAzureAccount_MissingTenantId() {
            // given
            RegisterAzureAccountRequest request = RegisterAzureAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(3L)
                    .accountId("12345678-1234-1234-1234-123456789012")
                    .accountName("azure-prod-account")
                    .authMethod(AuthMethod.SERVICE_PRINCIPAL)
                    .azureClientId("abcdef12-3456-7890-abcd-ef1234567890")
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAzureAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("azureTenantId는 필수입니다");
        }
        
        @Test
        @DisplayName("azureClientId가 없으면 검증 실패")
        void invalidAzureAccount_MissingClientId() {
            // given
            RegisterAzureAccountRequest request = RegisterAzureAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(3L)
                    .accountId("12345678-1234-1234-1234-123456789012")
                    .accountName("azure-prod-account")
                    .authMethod(AuthMethod.SERVICE_PRINCIPAL)
                    .azureTenantId("87654321-4321-4321-4321-210987654321")
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAzureAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("azureClientId는 필수입니다");
        }
        
        @Test
        @DisplayName("azureTenantId와 azureClientId가 모두 없으면 검증 실패")
        void invalidAzureAccount_MissingBothFields() {
            // given
            RegisterAzureAccountRequest request = RegisterAzureAccountRequest.builder()
                    .tenantId(1L)
                    .providerId(3L)
                    .accountId("12345678-1234-1234-1234-123456789012")
                    .accountName("azure-prod-account")
                    .authMethod(AuthMethod.SERVICE_PRINCIPAL)
                    .build();
            
            // when
            Set<ConstraintViolation<RegisterAzureAccountRequest>> violations = validator.validate(request);
            
            // then
            assertThat(violations).hasSize(1); // 첫 번째 검증 실패에서 멈춤
        }
    }
}

