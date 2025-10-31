package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.TenantCloudCredentials;
import com.agenticcp.core.domain.cloud.exception.AwsErrorCode;
import com.agenticcp.core.domain.cloud.repository.TenantCloudCredentialsRepository;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ThreadLocalCredentialCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.StsException;

import java.time.LocalDateTime;

/**
 * AWS 자격증명 제공자 포트 어댑터
 * 
 * 헥사고날 아키텍처의 아웃바운드 어댑터로, AWS 전용 자격증명 해결 기능을 제공합니다.
 * STS AssumeRole 패턴을 사용하여 테넌트별 AWS 자격증명을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AwsCredentialProviderPortAdapter implements CredentialProviderPort {

    private final TenantCloudCredentialsRepository credentialsRepository;
    private final StsClient stsClient;
    
    @Value("${aws.sts.session-duration:3600}")
    private int sessionDurationSeconds;
    
    @Value("${aws.sts.region:us-east-1}")
    private String defaultRegion;

    @Override
    public AwsCredentials resolveCredentials(String tenantKey, CloudProvider.ProviderType providerType, String accountScope) {
        try {
            log.debug("AWS 자격증명 해결 시작: tenantKey={}, accountScope={}", tenantKey, accountScope);
            
            // AWS가 아닌 경우 예외 발생
            if (providerType != CloudProvider.ProviderType.AWS) {
                throw new BusinessException(AwsErrorCode.AWS_CONFIGURATION_ERROR, 
                        "AWS 어댑터는 AWS 프로바이더만 지원합니다. 요청된 프로바이더: " + providerType);
            }
            
            // 1. 캐시에서 기존 자격증명 확인
            String cacheKey = generateCacheKey(tenantKey, providerType, accountScope);
            AwsCredentials cachedCredentials = ThreadLocalCredentialCache.getCredentials(cacheKey);
            
            if (cachedCredentials != null) {
                log.debug("캐시에서 AWS 자격증명 조회 성공: cacheKey={}", cacheKey);
                return cachedCredentials;
            }
            
            // 2. AWS 자격증명 해결
            AwsCredentials credentials = resolveAwsCredentials(tenantKey, accountScope);
            
            // 3. 자격증명을 캐시에 저장
            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(sessionDurationSeconds - 300); // 5분 여유
            ThreadLocalCredentialCache.putCredentials(cacheKey, credentials, expirationTime);
            
            log.info("AWS 자격증명 해결 완료: accountScope={}", accountScope);
            return credentials;
            
        } catch (Exception e) {
            log.error("AWS 자격증명 해결 실패: tenantKey={}, accountScope={}, error={}",
                    tenantKey, accountScope, e.getMessage(), e);
            throw new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID, 
                    "AWS 자격증명 해결에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * AWS 자격증명 해결 (STS AssumeRole 패턴)
     */
    private AwsCredentials resolveAwsCredentials(String tenantKey, String accountScope) {
        log.debug("AWS STS AssumeRole 시작: tenantKey={}, accountScope={}", tenantKey, accountScope);
        
        // 1. 테넌트별 IAM Role ARN 조회
        TenantCloudCredentials credentials = credentialsRepository
                .findByTenantKeyAndProviderTypeAndAccountScopeAndIsActiveTrue(
                        tenantKey, CloudProvider.ProviderType.AWS, accountScope)
                .orElseThrow(() -> new BusinessException(AwsErrorCode.AWS_STS_ROLE_NOT_FOUND, 
                        "테넌트의 AWS 자격증명을 찾을 수 없습니다: " + tenantKey));
        
        try {
            // 2. STS AssumeRole 요청 생성
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(credentials.getRoleArn())
                    .roleSessionName(credentials.getDefaultSessionName())
                    .durationSeconds(sessionDurationSeconds)
                    .externalId(credentials.getExternalId())
                    .build();
            
            // 3. STS AssumeRole 실행
            AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            Credentials awsCredentials = assumeRoleResponse.credentials();
            
            // 4. AwsSessionCredentials 생성
            AwsSessionCredentials sessionCredentials = AwsSessionCredentials.builder()
                    .accessKeyId(awsCredentials.accessKeyId())
                    .secretAccessKey(awsCredentials.secretAccessKey())
                    .sessionToken(awsCredentials.sessionToken())
                    .build();
            
            // 5. 사용 기록 업데이트
            credentials.updateLastUsed();
            credentialsRepository.save(credentials);
            
            log.info("AWS STS AssumeRole 성공: tenantKey={}, roleArn={}", 
                    tenantKey, credentials.getRoleArn());
            
            return sessionCredentials;
            
        } catch (StsException e) {
            // 오류 정보 업데이트
            credentials.updateError(e.awsErrorDetails().errorMessage());
            credentialsRepository.save(credentials);
            
            log.error("AWS STS AssumeRole 실패: tenantKey={}, roleArn={}, error={}", 
                    tenantKey, credentials.getRoleArn(), e.getMessage(), e);
            
            // AWS STS 에러를 비즈니스 예외로 변환
            throw convertStsException(e);
        }
    }
    
    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(String tenantKey, CloudProvider.ProviderType providerType, String accountScope) {
        return String.format("%s:%s:%s", tenantKey, providerType, accountScope);
    }
    
    /**
     * STS 예외를 비즈니스 예외로 변환
     */
    private BusinessException convertStsException(StsException e) {
        String errorCode = e.awsErrorDetails().errorCode();
        String errorMessage = e.awsErrorDetails().errorMessage();
        
        return switch (errorCode) {
            case "AccessDenied" -> new BusinessException(AwsErrorCode.AWS_STS_ACCESS_DENIED, errorMessage);
            case "InvalidUserID.NotFound" -> new BusinessException(AwsErrorCode.AWS_STS_ROLE_NOT_FOUND, errorMessage);
            case "ExternalIdMismatch" -> new BusinessException(AwsErrorCode.AWS_STS_EXTERNAL_ID_MISMATCH, errorMessage);
            case "ValidationError" -> new BusinessException(AwsErrorCode.AWS_STS_SESSION_DURATION_EXCEEDED, errorMessage);
            case "MalformedPolicyDocument" -> new BusinessException(AwsErrorCode.AWS_STS_MALFORMED_POLICY, errorMessage);
            case "RegionDisabledException" -> new BusinessException(AwsErrorCode.AWS_STS_REGION_MISMATCH, errorMessage);
            default -> new BusinessException(AwsErrorCode.AWS_STS_ASSUME_ROLE_FAILED, errorMessage);
        };
    }
}
