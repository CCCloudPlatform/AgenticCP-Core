package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum CloudErrorCode implements BaseErrorCode {
    // 리소스 관련
    CAPABILITY_NOT_DEFINED(HttpStatus.BAD_REQUEST, 4001, "지원 Capability가 정의되지 않았습니다."),
    UNSUPPORTED_OPERATION(HttpStatus.BAD_REQUEST, 4002, "지원되지 않는 작업입니다."),
    CREDENTIAL_NOT_FOUND(HttpStatus.UNAUTHORIZED, 4003, "자격증명을 찾을 수 없습니다."),
    PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, 4004, "클라우드 제공자 서비스와 통신할 수 없습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 4005, "호출 제한을 초과했습니다."),
    API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 4006, "클라우드 API 호출이 시간 초과되었습니다."),
    MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4007, "CSP 응답 매핑에 실패했습니다."),
    
    // 클라우드 계정 관련
    CLOUD_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 4101, "클라우드 계정을 찾을 수 없습니다."),
    DUPLICATE_CLOUD_ACCOUNT(HttpStatus.BAD_REQUEST, 4102, "이미 등록된 클라우드 계정입니다."),
    INVALID_ACCOUNT_ID_FORMAT(HttpStatus.BAD_REQUEST, 4103, "잘못된 계정 ID 형식입니다."),
    ACCOUNT_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, 4104, "계정 검증에 실패했습니다."),
    ACCOUNT_HAS_RESOURCES(HttpStatus.BAD_REQUEST, 4105, "연결된 리소스가 있어 삭제할 수 없습니다."),
    
    // CSP별 계정 검증
    INVALID_AWS_ACCOUNT_ID(HttpStatus.BAD_REQUEST, 4111, "잘못된 AWS 계정 ID입니다. 12자리 숫자여야 합니다."),
    INVALID_GCP_PROJECT_ID(HttpStatus.BAD_REQUEST, 4112, "잘못된 GCP 프로젝트 ID입니다. 6-30자 소문자, 숫자, 하이픈만 가능합니다."),
    INVALID_AZURE_SUBSCRIPTION_ID(HttpStatus.BAD_REQUEST, 4113, "잘못된 Azure Subscription ID입니다. UUID 형식이어야 합니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    CloudErrorCode(HttpStatus status, int codeNumber, String message) {
        this.httpStatus = status;
        this.codeNumber = codeNumber;
        this.message = message;
    }

    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public String getCode() { return "CLOUD_" + codeNumber; }
    @Override public String getMessage() { return message; }
}
