package com.agenticcp.core.common.dto.exception;

import org.springframework.http.HttpStatus;

/**
 * 표준 에러 응답 구성을 위한 에러 코드 인터페이스입니다.
 * HTTP 상태, 코드 문자열, 기본 메시지를 제공합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
