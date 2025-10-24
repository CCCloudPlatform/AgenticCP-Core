package com.agenticcp.core.domain.cloud.adapter.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.AccountMetadata;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationResult;
import com.agenticcp.core.domain.cloud.port.outbound.CloudAccountValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AWS 클라우드 계정 검증 어댑터
 * 
 * AWS SDK for Java v2를 사용하여 실제 AWS API를 호출합니다.
 * STS AssumeRole과 IAM 권한 확인을 통해 계정의 유효성을 검증합니다.
 * 
 * 주요 기능:
 * - STS AssumeRole을 통한 Cross-Account Access 검증
 * - IAM GetCallerIdentity를 통한 계정 정보 조회
 * - AWS 계정 메타데이터 수집
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsAccountValidationAdapter implements CloudAccountValidationPort {
    
    // AWS SDK 클라이언트들
    private final StsClient stsClient;
    private final IamClient iamClient;
    
    @Override
    public AccountValidationResult validateAccount(AccountValidationRequest request) {
        log.info("[AwsAccountValidationAdapter] validateAccount - accountId={}, region={}", 
                request.getAccountId(), request.getRegion());
        
        try {
            // 1. AWS 계정 ID 형식 검증
            if (!isValidAwsAccountId(request.getAccountId())) {
                return AccountValidationResult.failure(
                        request.getAccountId(),
                        ProviderType.AWS,
                        "AWS_INVALID_ACCOUNT_ID_FORMAT",
                        "Invalid AWS Account ID format. Must be 12 digits."
                );
            }
            
            // 2. STS AssumeRole 테스트
            AssumeRoleResponse assumeRoleResponse = testAssumeRole(request);
            
            // 3. IAM GetCallerIdentity로 계정 정보 확인
            software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity = getCallerIdentity(request);
            
            // 4. 계정 메타데이터 수집
            AccountMetadata metadata = collectAccountMetadata(request, callerIdentity);
            
            log.info("[AwsAccountValidationAdapter] validateAccount - success accountId={}", request.getAccountId());
            
            return AccountValidationResult.success(
                    request.getAccountId(),
                    ProviderType.AWS,
                    metadata
            );
            
        } catch (SdkException e) {
            log.error("[AwsAccountValidationAdapter] validateAccount - AWS SDK error accountId={}", 
                    request.getAccountId(), e);
            
            return AccountValidationResult.failure(
                    request.getAccountId(),
                    ProviderType.AWS,
                    "AWS_SDK_ERROR",
                    "AWS API call failed: " + e.getMessage()
            );
            
        } catch (Exception e) {
            log.error("[AwsAccountValidationAdapter] validateAccount - unexpected error accountId={}", 
                    request.getAccountId(), e);
            
            return AccountValidationResult.failure(
                    request.getAccountId(),
                    ProviderType.AWS,
                    "Unexpected error during validation: " + e.getMessage(),
                    "VALIDATION_ERROR"
            );
        }
    }
    
    @Override
    public AccountMetadata getAccountMetadata(AccountValidationRequest request) {
        log.info("[AwsAccountValidationAdapter] getAccountMetadata - accountId={}", request.getAccountId());
        
        try {
            software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity = getCallerIdentity(request);
            return collectAccountMetadata(request, callerIdentity);
            
        } catch (Exception e) {
            log.error("[AwsAccountValidationAdapter] getAccountMetadata - error accountId={}", 
                    request.getAccountId(), e);
            return null;
        }
    }
    
    @Override
    public AccountValidationResult testConnection(AccountValidationRequest request) {
        log.info("[AwsAccountValidationAdapter] testConnection - accountId={}", request.getAccountId());
        
        try {
            // 간단한 연결 테스트: GetCallerIdentity만 호출
            software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity = getCallerIdentity(request);
            
            if (callerIdentity != null && callerIdentity.account() != null) {
                return AccountValidationResult.success(
                        request.getAccountId(),
                        ProviderType.AWS,
                        null
                );
            } else {
                return AccountValidationResult.failure(
                        request.getAccountId(),
                        ProviderType.AWS,
                        "Unable to retrieve caller identity",
                        "CONNECTION_FAILED"
                );
            }
            
        } catch (Exception e) {
            log.error("[AwsAccountValidationAdapter] testConnection - error accountId={}", 
                    request.getAccountId(), e);
            
            return AccountValidationResult.failure(
                    request.getAccountId(),
                    ProviderType.AWS,
                    "AWS_CONNECTION_FAILED",
                    "Connection test failed: " + e.getMessage()
            );
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private boolean isValidAwsAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }
        
        // AWS Account ID는 12자리 숫자
        return accountId.matches("^\\d{12}$");
    }
    
    private AssumeRoleResponse testAssumeRole(AccountValidationRequest request) {
        log.debug("[AwsAccountValidationAdapter] testAssumeRole - roleArn={}", request.getRoleArn());
        
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(request.getRoleArn())
                .roleSessionName("AgenticCP-Validation-" + System.currentTimeMillis())
                .externalId(request.getExternalId())
                .durationSeconds(900) // 15분
                .build();
        
        return stsClient.assumeRole(assumeRoleRequest);
    }
    
    private software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse getCallerIdentity(AccountValidationRequest request) {
        log.debug("[AwsAccountValidationAdapter] getCallerIdentity - accountId={}", request.getAccountId());
        
        software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest getCallerIdentityRequest = 
                software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest.builder()
                .build();
        
        return stsClient.getCallerIdentity(getCallerIdentityRequest);
    }
    
    private AccountMetadata collectAccountMetadata(AccountValidationRequest request, 
                                                  software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse callerIdentity) {
        log.debug("[AwsAccountValidationAdapter] collectAccountMetadata - accountId={}", request.getAccountId());
        
        // AWS 리전 목록 (주요 리전들)
        List<String> availableRegions = List.of(
                "us-east-1", "us-east-2", "us-west-1", "us-west-2",
                "eu-west-1", "eu-west-2", "eu-west-3", "eu-central-1",
                "ap-northeast-1", "ap-northeast-2", "ap-southeast-1", "ap-southeast-2",
                "ca-central-1", "sa-east-1"
        );
        
        Map<String, Object> additionalInfo = Map.of(
                "userId", callerIdentity.userId() != null ? callerIdentity.userId() : "",
                "arn", callerIdentity.arn() != null ? callerIdentity.arn() : "",
                "roleArn", request.getRoleArn() != null ? request.getRoleArn() : "",
                "externalId", request.getExternalId() != null ? request.getExternalId() : ""
        );
        
        return AccountMetadata.builder()
                .providerType(ProviderType.AWS)
                .accountId(request.getAccountId())
                .accountName("AWS Account " + request.getAccountId()) // 실제로는 AWS Organizations API로 조회 가능
                .organizationId(null) // AWS Organizations API로 조회 가능
                .availableRegions(availableRegions)
                .customMetadata(additionalInfo)
                .build();
    }
}
