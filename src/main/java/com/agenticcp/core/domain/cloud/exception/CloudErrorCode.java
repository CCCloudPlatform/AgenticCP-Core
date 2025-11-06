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
    MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4007, "CSP 응답 매핑에 실패했습니다."),
    
    // Cloud Account 관련 에러 코드
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 4010, "클라우드 계정을 찾을 수 없습니다."),
    DUPLICATE_ACCOUNT(HttpStatus.CONFLICT, 4011, "이미 등록된 계정입니다."),
    ACCOUNT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, 4012, "계정 검증에 실패했습니다."),
    INVALID_ACCOUNT_STATUS(HttpStatus.BAD_REQUEST, 4013, "유효하지 않은 계정 상태입니다."),
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, 4014, "클라우드 프로바이더를 찾을 수 없습니다."),
    CREDENTIAL_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4015, "자격증명 암호화에 실패했습니다."),
    CREDENTIAL_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4016, "자격증명 복호화에 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 4017, "유효하지 않은 자격증명입니다."),
    CONNECTION_TEST_FAILED(HttpStatus.BAD_REQUEST, 4018, "연결 테스트에 실패했습니다."),
    ACCOUNT_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4019, "계정 정보 동기화에 실패했습니다."),
    ACCOUNT_DELETION_RESTRICTED(HttpStatus.CONFLICT, 4020, "연결된 리소스가 있어 계정을 삭제할 수 없습니다."),
    SESSION_ISSUANCE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4021, "세션 발급에 실패했습니다.");

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
