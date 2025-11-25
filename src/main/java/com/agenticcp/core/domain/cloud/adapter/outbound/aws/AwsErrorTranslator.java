package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.sts.model.StsException;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
public class AwsErrorTranslator {

    public RuntimeException translate(Throwable throwable) {
        if (throwable instanceof BusinessException businessException) {
            return businessException;
        }

        log.error("[AwsErrorTranslator] AWS error captured: {}", throwable.getMessage(), throwable);

        if (throwable instanceof StsException stsException) {
            return translateStsException(stsException);
        }

        if (throwable instanceof SdkClientException clientException) {
            return translateClientException(clientException);
        }

        if (throwable instanceof SdkServiceException serviceException) {
            return translateServiceException(serviceException);
        }

        if (throwable instanceof IllegalArgumentException illegalArgumentException) {
            return new BusinessException(CloudErrorCode.INVALID_REQUEST, illegalArgumentException.getMessage());
        }

        return new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
    }

    private RuntimeException translateClientException(SdkClientException exception) {
        String message = safeLower(exception.getMessage());

        if (message.contains("credential") || message.contains("access key") || message.contains("signature")) {
            return new BusinessException(CredentialErrorCode.INVALID_CREDENTIALS);
        }

        if (message.contains("connect") || message.contains("timed out") || message.contains("unreachable")) {
            return new BusinessException(CloudErrorCode.CLOUD_NETWORK_ERROR);
        }

        return new BusinessException(CloudErrorCode.CLOUD_CONFIGURATION_ERROR);
    }

    private RuntimeException translateServiceException(SdkServiceException exception) {
        AwsErrorDetails errorDetails = (exception instanceof AwsServiceException awsServiceException)
                ? awsServiceException.awsErrorDetails()
                : null;
        String errorCode = safeUpper(optionalAwsErrorCode(errorDetails));
        String message = safeLower(exception.getMessage());

        return switch (errorCode) {
            case "ACCESSDENIED", "ACCESSDENIEDEXCEPTION" ->
                    new BusinessException(CloudErrorCode.PERMISSION_DENIED);
            case "INVALIDCLIENTTOKENID", "INVALIDACCESSKEYID", "SIGNATUREDOESNOTMATCH", "UNRECOGNIZEDCLIENTEXCEPTION" ->
                    new BusinessException(CredentialErrorCode.INVALID_CREDENTIALS);
            case "THROTTLING", "THROTTLINGEXCEPTION", "REQUESTLIMITEXCEEDED" ->
                    new BusinessException(CloudErrorCode.API_RATE_LIMIT_EXCEEDED);
            case "SERVICEUNAVAILABLE", "INTERNALSERVICEERROR" ->
                    new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
            case "LIMITEXCEEDED", "LIMITEXCEEDEDEXCEPTION", "RESOURCELIMITEXCEEDED", "ACCOUNTLIMITEXCEEDED" ->
                    new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED);
            case "VALIDATIONERROR", "INVALIDPARAMETER", "INVALIDPARAMETERVALUE" ->
                    new BusinessException(CloudErrorCode.INVALID_REQUEST);
            case "REQUESTTIMEOUT", "REQUESTTIMEOUTEXCEPTION" ->
                    new BusinessException(CloudErrorCode.CLOUD_OPERATION_TIMEOUT);
            default -> inferFromMessage(message);
        };
    }

    private RuntimeException translateStsException(StsException exception) {
        String errorCode = safeUpper(optionalAwsErrorCode(exception.awsErrorDetails()));
        String message = safeLower(exception.getMessage());

        return switch (errorCode) {
            case "ACCESSDENIED", "ACCESSDENIEDEXCEPTION" ->
                    new BusinessException(CredentialErrorCode.ASSUME_ROLE_ACCESS_DENIED);
            case "NOSUCHENTITY", "NOSUCHENTITYEXCEPTION" ->
                    new BusinessException(CredentialErrorCode.ROLE_NOT_FOUND);
            case "MALFORMEDPOLICYDOCUMENT" ->
                    new BusinessException(CredentialErrorCode.MALFORMED_POLICY);
            case "REGIONDISABLEDEXCEPTION" ->
                    new BusinessException(CredentialErrorCode.REGION_MISMATCH);
            case "VALIDATIONERROR" ->
                    new BusinessException(CloudErrorCode.INVALID_REQUEST);
            case "THROTTLING", "THROTTLINGEXCEPTION" ->
                    new BusinessException(CloudErrorCode.API_RATE_LIMIT_EXCEEDED);
            case "EXPIREDTOKEN", "EXPIREDTOKENEXCEPTION" ->
                    new BusinessException(CredentialErrorCode.CREDENTIALS_EXPIRED);
            default -> inferStsFromMessage(message);
        };
    }

    private RuntimeException inferStsFromMessage(String message) {
        if (message.contains("external id")) {
            return new BusinessException(CredentialErrorCode.EXTERNAL_ID_MISMATCH);
        }
        if (message.contains("duration")) {
            return new BusinessException(CredentialErrorCode.SESSION_DURATION_EXCEEDED);
        }
        if (message.contains("role arn") || message.contains("assumerole")) {
            return new BusinessException(CredentialErrorCode.ROLE_NOT_FOUND);
        }
        return new BusinessException(CredentialErrorCode.TEMPORARY_SESSION_ISSUANCE_FAILED);
    }

    private RuntimeException inferFromMessage(String message) {
        if (message.contains("quota") || message.contains("limit")) {
            return new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED);
        }

        if (message.contains("region")) {
            return new BusinessException(CloudErrorCode.INVALID_REGION);
        }

        if (message.contains("access") || message.contains("permission")) {
            return new BusinessException(CloudErrorCode.PERMISSION_DENIED);
        }

        if (message.contains("credential")) {
            return new BusinessException(CredentialErrorCode.INVALID_CREDENTIALS);
        }

        return new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
    }

    private String optionalAwsErrorCode(AwsErrorDetails errorDetails) {
        return Optional.ofNullable(errorDetails)
                .map(AwsErrorDetails::errorCode)
                .orElse("");
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}

