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
    // 자격증명 관련 (4101-4109)
    CREDENTIAL_NOT_FOUND(HttpStatus.UNAUTHORIZED, 4101, "자격증명을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 4102, "유효하지 않은 자격증명입니다."),
    CREDENTIAL_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4103, "자격증명 암호화에 실패했습니다."),
    CREDENTIAL_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4104, "자격증명 복호화에 실패했습니다."),
    SESSION_ISSUANCE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4105, "세션 발급에 실패했습니다.");

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

