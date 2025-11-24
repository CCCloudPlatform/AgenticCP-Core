package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * 클라우드 도메인 일반 에러 코드
 * (S3, AWS 등 특정 서비스별 에러는 각각의 ErrorCode 클래스 참조)
 */
public enum CloudErrorCode implements BaseErrorCode {
    
    // 4000-4999: 클라우드 도메인
    CAPABILITY_NOT_DEFINED(HttpStatus.BAD_REQUEST, 4001, "지원 Capability가 정의되지 않았습니다."),
    UNSUPPORTED_OPERATION(HttpStatus.BAD_REQUEST, 4002, "지원되지 않는 작업입니다."),
    PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, 4003, "클라우드 제공자 서비스와 통신할 수 없습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 4004, "호출 제한을 초과했습니다."),
    API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 4005, "클라우드 API 호출이 시간 초과되었습니다."),
    MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4006, "CSP 응답 매핑에 실패했습니다."),
    
    CLOUD_PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, 4007, "클라우드 프로바이더를 찾을 수 없습니다."),
    CLOUD_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 4008, "클라우드 리소스를 찾을 수 없습니다."),
    CLOUD_REGION_NOT_FOUND(HttpStatus.NOT_FOUND, 4009, "클라우드 리전을 찾을 수 없습니다."),
    CLOUD_SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, 4010, "클라우드 서비스를 찾을 수 없습니다."),
    
    // Cloud Account 관련 (4011-4019)
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 4011, "클라우드 계정을 찾을 수 없습니다."),
    DUPLICATE_ACCOUNT(HttpStatus.CONFLICT, 4012, "이미 등록된 계정입니다."),
    ACCOUNT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, 4013, "계정 검증에 실패했습니다."),
    INVALID_ACCOUNT_STATUS(HttpStatus.BAD_REQUEST, 4014, "유효하지 않은 계정 상태입니다."),
    CONNECTION_TEST_FAILED(HttpStatus.BAD_REQUEST, 4015, "연결 테스트에 실패했습니다."),
    ACCOUNT_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4016, "계정 정보 동기화에 실패했습니다."),
    ACCOUNT_DELETION_RESTRICTED(HttpStatus.CONFLICT, 4017, "연결된 리소스가 있어 계정을 삭제할 수 없습니다."),
    ACCOUNT_SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, 4018, "AccountScope가 필요합니다."),
    ACCOUNT_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, 4019, "계정이 설정되지 않았습니다."),
    
    // 일반적인 클라우드 에러
    CLOUD_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 4030, "클라우드 서비스 연결에 실패했습니다."),
    CLOUD_OPERATION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 4031, "클라우드 작업 시간이 초과되었습니다."),
    CLOUD_RESOURCE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 4032, "클라우드 리소스 한도를 초과했습니다."),
    CLOUD_TAG_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4033, "클라우드 리소스 태그 작업이 실패했습니다."),
    CLOUD_METADATA_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4034, "클라우드 메타데이터 직렬화가 실패했습니다.");
    
    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    CloudErrorCode(HttpStatus status, int codeNumber, String message) {
        this.httpStatus = status;
        this.codeNumber = codeNumber;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return ErrorCategory.CLOUD.generate(codeNumber);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
