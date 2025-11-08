package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * AWS 관련 에러 코드
 */
public enum AwsErrorCode implements BaseErrorCode {
    
    // 4000-4999: 클라우드 도메인 내 AWS 서비스
    AWS_CREDENTIALS_INVALID(HttpStatus.UNAUTHORIZED, 4020, "AWS 자격 증명이 유효하지 않습니다."),
    AWS_REGION_INVALID(HttpStatus.BAD_REQUEST, 4021, "유효하지 않은 AWS 리전입니다."),
    AWS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 4022, "AWS API 호출 중 오류가 발생했습니다."),
    AWS_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 4023, "AWS 할당량을 초과했습니다."),
    AWS_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 4024, "AWS 서비스를 사용할 수 없습니다."),
    AWS_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4025, "AWS 리소스에 접근할 권한이 없습니다."),
    AWS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, 4026, "유효하지 않은 AWS 요청입니다."),
    AWS_THROTTLING(HttpStatus.TOO_MANY_REQUESTS, 4027, "AWS API 호출이 제한되었습니다."),
    AWS_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, 4028, "AWS 네트워크 연결에 실패했습니다."),
    AWS_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 4029, "AWS 설정에 오류가 있습니다."),
    
    // STS AssumeRole 관련 에러 코드
    AWS_STS_ASSUME_ROLE_FAILED(HttpStatus.UNAUTHORIZED, 4030, "AWS STS AssumeRole에 실패했습니다."),
    AWS_STS_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, 4031, "지정된 IAM Role을 찾을 수 없습니다."),
    AWS_STS_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4032, "IAM Role에 대한 AssumeRole 권한이 없습니다."),
    AWS_STS_EXTERNAL_ID_MISMATCH(HttpStatus.UNAUTHORIZED, 4033, "External ID가 일치하지 않습니다."),
    AWS_STS_SESSION_DURATION_EXCEEDED(HttpStatus.BAD_REQUEST, 4034, "세션 지속 시간이 최대값을 초과했습니다."),
    AWS_STS_CREDENTIALS_EXPIRED(HttpStatus.UNAUTHORIZED, 4035, "임시 자격증명이 만료되었습니다."),
    AWS_STS_MALFORMED_POLICY(HttpStatus.BAD_REQUEST, 4036, "IAM 정책이 잘못되었습니다."),
    AWS_STS_REGION_MISMATCH(HttpStatus.BAD_REQUEST, 4037, "리전이 일치하지 않습니다.");
    
    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    AwsErrorCode(HttpStatus status, int codeNumber, String message) {
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
