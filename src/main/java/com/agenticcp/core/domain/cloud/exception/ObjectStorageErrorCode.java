package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * 오브젝트 스토리지(S3, GCS, Blob 등) 공통 에러 코드
 */
public enum ObjectStorageErrorCode implements BaseErrorCode {

    BUCKET_NOT_FOUND(HttpStatus.NOT_FOUND, 4201, "오브젝트 스토리지 버킷을 찾을 수 없습니다."),
    BUCKET_ALREADY_EXISTS(HttpStatus.CONFLICT, 4202, "버킷이 이미 존재합니다."),
    BUCKET_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4203, "버킷에 접근할 권한이 없습니다."),
    INVALID_BUCKET_NAME(HttpStatus.BAD_REQUEST, 4204, "유효하지 않은 버킷 이름입니다."),
    BUCKET_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4205, "버킷 작업이 실패했습니다."),
    BUCKET_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4206, "버킷 생성에 실패했습니다."),
    BUCKET_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4207, "버킷 삭제에 실패했습니다."),
    BUCKET_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4208, "버킷 업데이트에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    ObjectStorageErrorCode(HttpStatus status, int codeNumber, String message) {
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

