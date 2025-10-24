package com.agenticcp.core.common.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 감사 로그 도메인 에러 코드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AuditErrorCode implements BaseErrorCode {
    
    // 8000-8999: 감사 로그 도메인
    AUDIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, 8001, "감사 로그를 찾을 수 없습니다."),
    INSUFFICIENT_AUDIT_PERMISSION(HttpStatus.FORBIDDEN, 8002, "감사 로그 조회 권한이 없습니다."),
    TENANT_ACCESS_DENIED(HttpStatus.FORBIDDEN, 8003, "테넌트 접근이 거부되었습니다."),
    INVALID_TENANT_CONTEXT(HttpStatus.BAD_REQUEST, 8004, "유효하지 않은 테넌트 컨텍스트입니다."),
    AUDIT_LOG_EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8005, "감사 로그 내보내기에 실패했습니다."),
    AUDIT_LOG_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8006, "감사 로그 검색에 실패했습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, 8007, "유효하지 않은 날짜 범위입니다."),
    AUDIT_LOG_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8008, "감사 로그 변환에 실패했습니다.");
    
    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.MONITORING.generate(codeNumber);
    }
}
