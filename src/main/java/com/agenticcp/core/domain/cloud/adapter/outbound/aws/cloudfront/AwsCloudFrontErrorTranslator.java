package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsErrorTranslator;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.cloudfront.model.*;

/**
 * AWS CloudFront 관련 예외 변환기
 * AWS CloudFront SDK에서 발생하는 예외를 도메인 예외로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsCloudFrontErrorTranslator {

    private final AwsErrorTranslator awsErrorTranslator;

    /**
     * AWS CloudFront 관련 예외를 도메인 예외로 변환합니다.
     * 
     * 변환 규칙:
     * - NoSuchDistributionException → ResourceNotFoundException (DISTRIBUTION_NOT_FOUND)
     * - DistributionAlreadyExistsException → BusinessException (DISTRIBUTION_ALREADY_EXISTS)
     * - CloudFrontException의 errorCode에 따라 적절한 예외로 변환
     * - 기타 예외는 AwsErrorTranslator를 통해 변환
     *
     * @param e 변환할 예외
     * @return 변환된 도메인 예외 (BusinessException 또는 ResourceNotFoundException)
     */
    public RuntimeException translate(Exception e) {
        log.error("[AwsCloudFrontErrorTranslator] CloudFront operation failed: {}", e.getMessage(), e);

        if (e instanceof NoSuchDistributionException) {
            return new ResourceNotFoundException(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND);
        }

        if (e instanceof CloudFrontException) {
            CloudFrontException cfException = (CloudFrontException) e;
            String errorCode = cfException.awsErrorDetails() != null 
                ? cfException.awsErrorDetails().errorCode() 
                : "";

            return switch (errorCode) {
                case "NoSuchDistribution" -> 
                    new ResourceNotFoundException(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND);
                case "DistributionAlreadyExists" -> 
                    new BusinessException(CloudErrorCode.INVALID_REQUEST, "Distribution already exists");
                case "InvalidIfMatchVersion" -> 
                    new BusinessException(CloudErrorCode.INVALID_REQUEST, 
                        "Distribution was modified by another request. Please retry with the latest ETag.");
                case "PreconditionFailed" -> 
                    new BusinessException(CloudErrorCode.INVALID_REQUEST, 
                        "Distribution configuration was changed. Please retry with the latest ETag.");
                case "IllegalUpdate" -> 
                    new BusinessException(CloudErrorCode.INVALID_REQUEST, 
                        "Cannot update distribution: " + cfException.getMessage());
                case "TooManyDistributions" -> 
                    new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED, 
                        "Distribution limit exceeded");
                case "TooManyInvalidationsInProgress" -> 
                    new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED, 
                        "Too many invalidations in progress");
                case "AccessDenied" -> 
                    new BusinessException(CloudErrorCode.PERMISSION_DENIED, 
                        "Access denied to CloudFront resource");
                case "ServiceUnavailable" -> 
                    new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
                case "ThrottlingException" -> 
                    new BusinessException(CloudErrorCode.API_RATE_LIMIT_EXCEEDED);
                default -> 
                    new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE, 
                        "CloudFront operation failed: " + errorCode);
            };
        }

        if (e instanceof SdkClientException) {
            return awsErrorTranslator.translate(e);
        }

        if (e instanceof SdkServiceException) {
            return awsErrorTranslator.translate(e);
        }

        if (e instanceof BusinessException) {
            return (BusinessException) e;
        }

        return awsErrorTranslator.translate(e);
    }
}

