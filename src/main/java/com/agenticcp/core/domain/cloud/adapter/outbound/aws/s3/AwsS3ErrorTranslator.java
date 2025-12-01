package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsErrorTranslator;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.ObjectStorageErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * AWS S3 관련 예외 변환기
 * AWS S3 SDK에서 발생하는 예외를 도메인 예외로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3ErrorTranslator {

    private final AwsErrorTranslator awsErrorTranslator;

    /**
     * AWS S3 관련 예외를 도메인 예외로 변환합니다.
     * 
     * 변환 규칙:
     * - NoSuchBucketException → ResourceNotFoundException (BUCKET_NOT_FOUND)
     * - BucketAlreadyExistsException → BusinessException (BUCKET_ALREADY_EXISTS)
     * - S3Exception의 errorCode에 따라 적절한 예외로 변환
     * - 기타 예외는 AwsErrorTranslator를 통해 변환
     *
     * @param e 변환할 예외
     * @return 변환된 도메인 예외 (BusinessException 또는 ResourceNotFoundException)
     */
    public RuntimeException translate(Exception e) {
        log.error("[AwsS3ErrorTranslator] S3 operation failed: {}", e.getMessage(), e);

        if (e instanceof NoSuchBucketException) {
            return new ResourceNotFoundException(ObjectStorageErrorCode.BUCKET_NOT_FOUND);
        }

        if (e instanceof BucketAlreadyExistsException) {
            return new BusinessException(ObjectStorageErrorCode.BUCKET_ALREADY_EXISTS);
        }

        if (e instanceof S3Exception) {
            S3Exception s3Exception = (S3Exception) e;
            String errorCode = s3Exception.awsErrorDetails().errorCode();

            return switch (errorCode) {
                case "NoSuchBucket" -> new ResourceNotFoundException(ObjectStorageErrorCode.BUCKET_NOT_FOUND);
                case "BucketAlreadyExists" -> new BusinessException(ObjectStorageErrorCode.BUCKET_ALREADY_EXISTS);
                case "AccessDenied" -> new BusinessException(ObjectStorageErrorCode.BUCKET_ACCESS_DENIED);
                case "InvalidBucketName" -> new BusinessException(ObjectStorageErrorCode.INVALID_BUCKET_NAME);
                case "ServiceUnavailable" -> new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
                case "ThrottlingException" -> new BusinessException(CloudErrorCode.API_RATE_LIMIT_EXCEEDED);
                default -> new BusinessException(ObjectStorageErrorCode.BUCKET_OPERATION_FAILED);
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
