package com.agenticcp.core.domain.cloud.validation;

import com.agenticcp.core.domain.cloud.dto.RegisterAwsAccountRequest;
import com.agenticcp.core.domain.cloud.dto.RegisterAzureAccountRequest;
import com.agenticcp.core.domain.cloud.dto.RegisterCloudAccountRequest;
import com.agenticcp.core.domain.cloud.dto.RegisterGcpAccountRequest;
import com.agenticcp.core.domain.cloud.entity.CloudAccount.AuthMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 클라우드 계정 등록 요청 클래스 레벨 Validator 구현체
 * 
 * CSP별 필수 필드를 검증합니다:
 * - AWS: roleArn (IAM_ROLE 인증 방식 사용 시 필수)
 * - GCP: serviceAccountEmail (필수)
 * - Azure: azureTenantId, azureClientId (필수)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public class CloudAccountRequestValidator implements ConstraintValidator<ValidCloudAccountRequest, RegisterCloudAccountRequest> {
    
    @Override
    public void initialize(ValidCloudAccountRequest constraintAnnotation) {
        // 초기화 로직 없음
    }
    
    @Override
    public boolean isValid(RegisterCloudAccountRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        
        // AWS 검증
        if (request instanceof RegisterAwsAccountRequest) {
            RegisterAwsAccountRequest awsRequest = (RegisterAwsAccountRequest) request;
            
            // IAM_ROLE 인증 방식 사용 시 roleArn 필수
            if (AuthMethod.IAM_ROLE.equals(awsRequest.getAuthMethod())) {
                if (awsRequest.getRoleArn() == null || awsRequest.getRoleArn().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                        "AWS IAM_ROLE 인증 방식을 사용할 때 roleArn은 필수입니다"
                    ).addPropertyNode("roleArn").addConstraintViolation();
                    return false;
                }
            }
        }
        
        // GCP 검증
        if (request instanceof RegisterGcpAccountRequest) {
            RegisterGcpAccountRequest gcpRequest = (RegisterGcpAccountRequest) request;
            
            // Service Account Email 필수
            if (gcpRequest.getServiceAccountEmail() == null || gcpRequest.getServiceAccountEmail().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "GCP 계정 등록 시 serviceAccountEmail은 필수입니다"
                ).addPropertyNode("serviceAccountEmail").addConstraintViolation();
                return false;
            }
        }
        
        // Azure 검증
        if (request instanceof RegisterAzureAccountRequest) {
            RegisterAzureAccountRequest azureRequest = (RegisterAzureAccountRequest) request;
            
            // Azure Tenant ID 필수
            if (azureRequest.getAzureTenantId() == null || azureRequest.getAzureTenantId().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Azure 계정 등록 시 azureTenantId는 필수입니다"
                ).addPropertyNode("azureTenantId").addConstraintViolation();
                return false;
            }
            
            // Azure Client ID 필수
            if (azureRequest.getAzureClientId() == null || azureRequest.getAzureClientId().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Azure 계정 등록 시 azureClientId는 필수입니다"
                ).addPropertyNode("azureClientId").addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
}

