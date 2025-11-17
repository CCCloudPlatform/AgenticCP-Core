package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * S3 관련 에러 코드
 */
public enum S3ErrorCode implements BaseErrorCode {
    
    // 4000-4999: 클라우드 도메인 내 S3 서비스
    S3_BUCKET_NOT_FOUND(HttpStatus.NOT_FOUND, 4012, "S3 버킷을 찾을 수 없습니다."),
    S3_BUCKET_ALREADY_EXISTS(HttpStatus.CONFLICT, 4013, "S3 버킷이 이미 존재합니다."),
    S3_BUCKET_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4014, "S3 버킷에 접근할 권한이 없습니다."),
    S3_BUCKET_INVALID_NAME(HttpStatus.BAD_REQUEST, 4015, "유효하지 않은 S3 버킷 이름입니다."),
    S3_BUCKET_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4016, "S3 버킷 작업이 실패했습니다."),
    S3_BUCKET_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4017, "S3 버킷 생성에 실패했습니다."),
    S3_BUCKET_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4018, "S3 버킷 삭제에 실패했습니다."),
    S3_BUCKET_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4019, "S3 버킷 업데이트에 실패했습니다.");
    
    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    S3ErrorCode(HttpStatus status, int codeNumber, String message) {
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
