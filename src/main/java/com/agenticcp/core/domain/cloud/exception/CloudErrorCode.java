package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum CloudErrorCode implements BaseErrorCode {
    CAPABILITY_NOT_DEFINED(HttpStatus.BAD_REQUEST, 4001, "지원 Capability가 정의되지 않았습니다."),
    UNSUPPORTED_OPERATION(HttpStatus.BAD_REQUEST, 4002, "지원되지 않는 작업입니다."),
    CREDENTIAL_NOT_FOUND(HttpStatus.UNAUTHORIZED, 4003, "자격증명을 찾을 수 없습니다."),
    PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, 4004, "클라우드 제공자 서비스와 통신할 수 없습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 4005, "호출 제한을 초과했습니다."),
    API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 4006, "클라우드 API 호출이 시간 초과되었습니다."),
    MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4007, "CSP 응답 매핑에 실패했습니다.");

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
