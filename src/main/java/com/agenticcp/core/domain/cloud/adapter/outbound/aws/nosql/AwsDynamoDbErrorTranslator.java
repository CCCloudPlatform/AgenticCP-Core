package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsErrorTranslator;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.dynamodb.model.*;

/**
 * AWS DynamoDB 관련 예외 변환기
 * AWS DynamoDB SDK에서 발생하는 예외를 도메인 예외로 변환합니다.
 * 
 * 변환 규칙:
 * - ResourceNotFoundException → NOSQL_TABLE_NOT_FOUND
 * - ResourceInUseException → NOSQL_TABLE_ALREADY_EXISTS
 * - ProvisionedThroughputExceededException → NOSQL_THROTTLED
 * - RequestLimitExceededException → NOSQL_THROTTLED
 * - LimitExceededException → RESOURCE_QUOTA_EXCEEDED
 * - ValidationException → NOSQL_VALIDATION_ERROR
 * - 기타 → AwsErrorTranslator에 위임
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbErrorTranslator {

    private final AwsErrorTranslator awsErrorTranslator;

    /**
     * AWS DynamoDB 관련 예외를 도메인 예외로 변환합니다.
     * 
     * @param e 변환할 예외
     * @return 변환된 도메인 예외 (BusinessException 또는 ResourceNotFoundException)
     */
    public RuntimeException translate(Exception e) {
        log.error("[AwsDynamoDbErrorTranslator] DynamoDB operation failed: {}", e.getMessage(), e);

        // DynamoDB 전용 예외 처리
        if (e instanceof ResourceNotFoundException) {
            return new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        }

        if (e instanceof ResourceInUseException) {
            return new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS);
        }

        if (e instanceof TableAlreadyExistsException) {
            return new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS);
        }

        if (e instanceof TableNotFoundException) {
            return new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        }

        if (e instanceof ProvisionedThroughputExceededException) {
            return new BusinessException(CloudErrorCode.NOSQL_THROTTLED,
                    "프로비저닝된 처리량을 초과했습니다. 잠시 후 다시 시도해주세요.");
        }

        if (e instanceof RequestLimitExceededException) {
            return new BusinessException(CloudErrorCode.NOSQL_THROTTLED,
                    "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }

        if (e instanceof LimitExceededException) {
            return new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED,
                    "DynamoDB 리소스 한도를 초과했습니다.");
        }

        if (e instanceof ConditionalCheckFailedException) {
            return new BusinessException(CloudErrorCode.NOSQL_VALIDATION_ERROR,
                    "조건부 검사에 실패했습니다.");
        }

        if (e instanceof TransactionCanceledException) {
            return new BusinessException(CloudErrorCode.NOSQL_VALIDATION_ERROR,
                    "트랜잭션이 취소되었습니다.");
        }

        if (e instanceof ItemCollectionSizeLimitExceededException) {
            return new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED,
                    "아이템 컬렉션 크기 제한을 초과했습니다.");
        }

        if (e instanceof BackupInUseException) {
            return new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "백업이 사용 중입니다.");
        }

        if (e instanceof BackupNotFoundException) {
            return new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED);
        }

        if (e instanceof ContinuousBackupsUnavailableException) {
            return new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "연속 백업을 사용할 수 없습니다.");
        }

        if (e instanceof PointInTimeRecoveryUnavailableException) {
            return new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "시점 복구를 사용할 수 없습니다.");
        }

        if (e instanceof GlobalTableAlreadyExistsException) {
            return new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS,
                    "글로벌 테이블이 이미 존재합니다.");
        }

        if (e instanceof GlobalTableNotFoundException) {
            return new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        }

        if (e instanceof ReplicaAlreadyExistsException) {
            return new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS,
                    "복제본이 이미 존재합니다.");
        }

        if (e instanceof ReplicaNotFoundException) {
            return new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        }

        if (e instanceof IndexNotFoundException) {
            return new BusinessException(CloudErrorCode.NOSQL_INDEX_OPERATION_FAILED,
                    "인덱스를 찾을 수 없습니다.");
        }

        if (e instanceof IdempotentParameterMismatchException) {
            return new BusinessException(CloudErrorCode.NOSQL_VALIDATION_ERROR,
                    "멱등성 파라미터가 일치하지 않습니다.");
        }

        // DynamoDB Exception (일반)
        if (e instanceof DynamoDbException dynamoDbException) {
            return translateDynamoDbException(dynamoDbException);
        }

        // SDK 클라이언트 예외
        if (e instanceof SdkClientException) {
            return awsErrorTranslator.translate(e);
        }

        // SDK 서비스 예외
        if (e instanceof SdkServiceException) {
            return awsErrorTranslator.translate(e);
        }

        // 이미 도메인 예외인 경우
        if (e instanceof BusinessException) {
            return (BusinessException) e;
        }

        // 기타 예외는 공통 변환기에 위임
        return awsErrorTranslator.translate(e);
    }

    /**
     * DynamoDbException을 에러 코드 기반으로 변환합니다.
     */
    private RuntimeException translateDynamoDbException(DynamoDbException e) {
        String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
        String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();

        log.debug("[AwsDynamoDbErrorTranslator] Translating DynamoDbException: errorCode={}, message={}",
                errorCode, errorMessage);

        return switch (errorCode) {
            case "ResourceNotFoundException" ->
                    new com.agenticcp.core.common.exception.ResourceNotFoundException(
                            CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
            case "ResourceInUseException", "TableAlreadyExistsException" ->
                    new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS);
            case "ProvisionedThroughputExceededException", "RequestLimitExceeded", "ThrottlingException" ->
                    new BusinessException(CloudErrorCode.NOSQL_THROTTLED,
                            "요청이 스로틀링되었습니다. 잠시 후 다시 시도해주세요.");
            case "LimitExceededException" ->
                    new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED,
                            "DynamoDB 리소스 한도를 초과했습니다.");
            case "ValidationException", "SerializationException" ->
                    new BusinessException(CloudErrorCode.NOSQL_VALIDATION_ERROR,
                            "요청 검증에 실패했습니다: " + errorMessage);
            case "AccessDeniedException" ->
                    new BusinessException(CloudErrorCode.PERMISSION_DENIED,
                            "DynamoDB 리소스에 대한 접근이 거부되었습니다.");
            case "UnrecognizedClientException", "InvalidSignatureException" ->
                    new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED,
                            "AWS 자격증명이 유효하지 않습니다.");
            case "InternalServerError" ->
                    new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE,
                            "AWS DynamoDB 내부 서버 오류가 발생했습니다.");
            case "ServiceUnavailable" ->
                    new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE,
                            "AWS DynamoDB 서비스를 사용할 수 없습니다.");
            default ->
                    new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE,
                            "DynamoDB 작업 중 오류가 발생했습니다: " + errorMessage);
        };
    }
}

