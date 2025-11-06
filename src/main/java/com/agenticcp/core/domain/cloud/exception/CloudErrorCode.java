package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 클라우드 공통 에러 코드
 * 클라우드 계정, 프로바이더, Capability 등 공통 클라우드 관련 에러를 정의합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public enum CloudErrorCode implements BaseErrorCode {
    // Capability 관련 (4001-4009)
    CAPABILITY_NOT_DEFINED(HttpStatus.BAD_REQUEST, 4001, "지원 Capability가 정의되지 않았습니다."),
    UNSUPPORTED_OPERATION(HttpStatus.BAD_REQUEST, 4002, "지원되지 않는 작업입니다."),
    
    // 프로바이더 관련 (4010-4019)
    PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, 4010, "클라우드 제공자 서비스와 통신할 수 없습니다."),
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, 4011, "클라우드 프로바이더를 찾을 수 없습니다."),
    
    // API 호출 관련 (4020-4029)
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 4020, "호출 제한을 초과했습니다."),
    API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 4021, "클라우드 API 호출이 시간 초과되었습니다."),
    MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4022, "CSP 응답 매핑에 실패했습니다."),
    
    // Cloud Account 관련 (4030-4049)
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 4030, "클라우드 계정을 찾을 수 없습니다."),
    DUPLICATE_ACCOUNT(HttpStatus.CONFLICT, 4031, "이미 등록된 계정입니다."),
    ACCOUNT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, 4032, "계정 검증에 실패했습니다."),
    INVALID_ACCOUNT_STATUS(HttpStatus.BAD_REQUEST, 4033, "유효하지 않은 계정 상태입니다."),
    CONNECTION_TEST_FAILED(HttpStatus.BAD_REQUEST, 4034, "연결 테스트에 실패했습니다."),
    ACCOUNT_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4035, "계정 정보 동기화에 실패했습니다."),
    ACCOUNT_DELETION_RESTRICTED(HttpStatus.CONFLICT, 4036, "연결된 리소스가 있어 계정을 삭제할 수 없습니다.");

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
