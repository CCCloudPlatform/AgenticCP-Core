package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.exception.AwsErrorCode;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.S3ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
public class AwsS3ErrorTranslator {

    /**
     * AWS S3 관련 예외를 도메인 예외로 변환합니다.
     */
    public RuntimeException translate(Exception e) {
        log.error("[AwsS3ErrorTranslator] S3 operation failed: {}", e.getMessage(), e);

        if (e instanceof NoSuchBucketException) {
            return new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
        }

        if (e instanceof BucketAlreadyExistsException) {
            return new BusinessException(S3ErrorCode.S3_BUCKET_ALREADY_EXISTS);
        }

        if (e instanceof S3Exception) {
            S3Exception s3Exception = (S3Exception) e;
            String errorCode = s3Exception.awsErrorDetails().errorCode();

            return switch (errorCode) {
                case "NoSuchBucket" -> new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
                case "BucketAlreadyExists" -> new BusinessException(S3ErrorCode.S3_BUCKET_ALREADY_EXISTS);
                case "AccessDenied" -> new BusinessException(S3ErrorCode.S3_BUCKET_ACCESS_DENIED);
                case "InvalidBucketName" -> new BusinessException(S3ErrorCode.S3_BUCKET_INVALID_NAME);
                case "ServiceUnavailable" -> new BusinessException(AwsErrorCode.AWS_SERVICE_UNAVAILABLE);
                case "ThrottlingException" -> new BusinessException(AwsErrorCode.AWS_QUOTA_EXCEEDED);
                default -> new BusinessException(S3ErrorCode.S3_BUCKET_OPERATION_FAILED);
            };
        }

        if (e instanceof SdkClientException) {
            return new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID);
        }

        if (e instanceof SdkServiceException) {
            return new BusinessException(AwsErrorCode.AWS_API_ERROR);
        }

        if (e instanceof BusinessException) {
            return (BusinessException) e;
        }

        return new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED);
    }
}
