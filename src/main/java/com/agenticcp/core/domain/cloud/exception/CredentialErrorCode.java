package com.agenticcp.core.domain.cloud.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 자격증명 관련 에러 코드
 * 자격증명 저장, 조회, 암호화/복호화, 세션 발급 등과 관련된 에러를 정의합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public enum CredentialErrorCode implements BaseErrorCode {
    // 자격증명 관련 (4101-4112)
    CREDENTIAL_NOT_FOUND(HttpStatus.UNAUTHORIZED, 4101, "자격증명을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 4102, "유효하지 않은 자격증명입니다."),
    CREDENTIAL_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4103, "자격증명 암호화에 실패했습니다."),
    CREDENTIAL_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4104, "자격증명 복호화에 실패했습니다."),
    TEMPORARY_SESSION_ISSUANCE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4105, "임시 세션 발급에 실패했습니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, 4106, "역할을 찾을 수 없습니다."),
    ASSUME_ROLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4107, "임시 세션 발급에 필요한 역할 권한이 없습니다."),
    EXTERNAL_ID_MISMATCH(HttpStatus.UNAUTHORIZED, 4108, "External ID가 일치하지 않습니다."),
    SESSION_DURATION_EXCEEDED(HttpStatus.BAD_REQUEST, 4109, "세션 지속 시간이 허용치를 초과했습니다."),
    CREDENTIALS_EXPIRED(HttpStatus.UNAUTHORIZED, 4110, "자격증명이 만료되었습니다."),
    MALFORMED_POLICY(HttpStatus.BAD_REQUEST, 4111, "IAM 정책이 잘못되었습니다."),
    REGION_MISMATCH(HttpStatus.BAD_REQUEST, 4112, "자격증명 리전이 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    CredentialErrorCode(HttpStatus status, int codeNumber, String message) {
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
        return "CREDENTIAL_" + codeNumber; 
    }
    
    @Override 
    public String getMessage() { 
        return message; 
    }
}

