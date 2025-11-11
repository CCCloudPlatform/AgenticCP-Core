package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.dto.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.dto.AccountValidationResult;
import com.agenticcp.core.domain.cloud.dto.ConnectionTestResult;
import com.agenticcp.core.domain.cloud.port.outbound.AccountValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS 계정 검증 어댑터
 * AWS STS GetCallerIdentity API를 사용하여 계정을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsAccountValidationAdapter implements AccountValidationPort {

    private final AwsClientConfig awsClientConfig;

    /**
     * AWS 계정 자격증명의 유효성을 검증합니다.
     * STS GetCallerIdentity API를 호출하여 자격증명이 유효한지 확인합니다.
     * 
     * @param request 검증 요청 정보
     * @return AccountValidationResult 검증 결과
     */
    @Override
    public AccountValidationResult validateAccount(AccountValidationRequest request) {
        log.info("[AwsAccountValidationAdapter] validateAccount - providerType={}, region={}", 
                 request.getProviderType(), request.getRegion());
        
        StsClient stsClient = null;
        
        try {
            // 동적 자격증명으로 STS Client 생성
            String region = request.getRegion() != null ? request.getRegion() : "us-east-1";
            stsClient = awsClientConfig.createStsClient(
                request.getAccessKeyId(), 
                request.getSecretAccessKey(), 
                region
            );
            
            // GetCallerIdentity API 호출
            GetCallerIdentityRequest callerIdentityRequest = GetCallerIdentityRequest.builder().build();
            GetCallerIdentityResponse response = stsClient.getCallerIdentity(callerIdentityRequest);
            
            // 메타데이터 구성
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("arn", response.arn());
            metadata.put("userId", response.userId());
            metadata.put("accountId", response.account());
            
            log.info("[AwsAccountValidationAdapter] validateAccount - success, accountId={}", 
                     response.account());
            
            // 검증 성공 결과 반환
            return AccountValidationResult.builder()
                    .valid(true)
                    .message("AWS 계정 검증 성공")
                    .accountId(response.account())
                    .region(region)
                    .metadata(metadata)
                    .build();
                    
        } catch (SdkException e) {
            log.error("[AwsAccountValidationAdapter] validateAccount - AWS API error", e);
            
            // AWS SDK 예외를 비즈니스 예외로 변환
            String errorMessage = parseAwsErrorMessage(e);
            
            return AccountValidationResult.builder()
                    .valid(false)
                    .message(errorMessage)
                    .build();
                    
        } catch (Exception e) {
            log.error("[AwsAccountValidationAdapter] validateAccount - unexpected error", e);
            
            return AccountValidationResult.builder()
                    .valid(false)
                    .message("예상치 못한 오류가 발생했습니다: " + e.getMessage())
                    .build();
                    
        } finally {
            // STS Client 종료
            if (stsClient != null) {
                try {
                    stsClient.close();
                } catch (Exception e) {
                    log.warn("[AwsAccountValidationAdapter] Failed to close STS client", e);
                }
            }
        }
    }

    /**
     * 등록된 계정의 연결을 테스트합니다.
     * 
     * @param accountId 계정 ID
     * @param credentials 자격증명 정보
     * @return ConnectionTestResult 연결 테스트 결과
     */
    @Override
    public ConnectionTestResult testConnection(Long accountId, Map<String, String> credentials) {
        log.info("[AwsAccountValidationAdapter] testConnection - accountId={}", accountId);
        
        String accessKeyId = credentials.get("accessKeyId");
        String secretAccessKey = credentials.get("secretAccessKey");
        String region = credentials.getOrDefault("region", "us-east-1");
        
        StsClient stsClient = null;
        long startTime = System.currentTimeMillis();
        
        try {
            // 동적 자격증명으로 STS Client 생성
            stsClient = awsClientConfig.createStsClient(accessKeyId, secretAccessKey, region);
            
            // GetCallerIdentity API 호출
            GetCallerIdentityRequest request = GetCallerIdentityRequest.builder().build();
            GetCallerIdentityResponse response = stsClient.getCallerIdentity(request);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 상세 정보 구성
            Map<String, Object> details = new HashMap<>();
            details.put("accountId", response.account());
            details.put("arn", response.arn());
            details.put("userId", response.userId());
            details.put("region", region);
            details.put("responseTimeMs", responseTime);
            
            log.info("[AwsAccountValidationAdapter] testConnection - success, responseTime={}ms", 
                     responseTime);
            
            return ConnectionTestResult.builder()
                    .success(true)
                    .message("AWS 계정 연결 성공")
                    .accountId(accountId)
                    .testedAt(LocalDateTime.now())
                    .details(details)
                    .build();
                    
        } catch (SdkException e) {
            log.error("[AwsAccountValidationAdapter] testConnection - AWS API error", e);
            
            String errorMessage = parseAwsErrorMessage(e);
            
            Map<String, Object> details = new HashMap<>();
            details.put("error", errorMessage);
            details.put("region", region);
            
            return ConnectionTestResult.builder()
                    .success(false)
                    .message("AWS 계정 연결 실패: " + errorMessage)
                    .accountId(accountId)
                    .testedAt(LocalDateTime.now())
                    .details(details)
                    .build();
                    
        } catch (Exception e) {
            log.error("[AwsAccountValidationAdapter] testConnection - unexpected error", e);
            
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());
            
            return ConnectionTestResult.builder()
                    .success(false)
                    .message("예상치 못한 오류가 발생했습니다: " + e.getMessage())
                    .accountId(accountId)
                    .testedAt(LocalDateTime.now())
                    .details(details)
                    .build();
                    
        } finally {
            // STS Client 종료
            if (stsClient != null) {
                try {
                    stsClient.close();
                } catch (Exception e) {
                    log.warn("[AwsAccountValidationAdapter] Failed to close STS client", e);
                }
            }
        }
    }

    /**
     * AWS SDK 예외 메시지를 파싱하여 사용자 친화적인 메시지로 변환합니다.
     * 
     * @param e AWS SDK 예외
     * @return 파싱된 에러 메시지
     */
    private String parseAwsErrorMessage(SdkException e) {
        String message = e.getMessage();
        
        if (message == null) {
            return "AWS API 호출 중 오류가 발생했습니다";
        }
        
        // 일반적인 AWS 에러 패턴 처리
        if (message.contains("InvalidClientTokenId") || message.contains("SignatureDoesNotMatch")) {
            return "유효하지 않은 AWS 자격증명입니다. Access Key ID와 Secret Access Key를 확인해주세요";
        } else if (message.contains("AccessDenied")) {
            return "AWS 계정 접근이 거부되었습니다. IAM 권한을 확인해주세요";
        } else if (message.contains("RequestExpired")) {
            return "AWS 요청이 만료되었습니다. 시스템 시간을 확인해주세요";
        } else if (message.contains("ServiceUnavailable")) {
            return "AWS 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요";
        } else if (message.contains("NetworkError") || message.contains("UnknownHost")) {
            return "네트워크 연결 오류가 발생했습니다. 인터넷 연결을 확인해주세요";
        }
        
        // 기본 메시지
        return "AWS 계정 검증 실패: " + message;
    }
}

