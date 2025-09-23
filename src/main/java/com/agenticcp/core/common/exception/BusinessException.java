package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.enums.AuthErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public BusinessException(AuthErrorCode authErrorCode) {
        super(authErrorCode.getMessage());
        this.errorCode = authErrorCode.getCode();
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    public BusinessException(AuthErrorCode authErrorCode, HttpStatus httpStatus) {
        super(authErrorCode.getMessage());
        this.errorCode = authErrorCode.getCode();
        this.httpStatus = httpStatus;
    }
}
