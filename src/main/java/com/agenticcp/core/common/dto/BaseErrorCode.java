package com.agenticcp.core.common.dto;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
