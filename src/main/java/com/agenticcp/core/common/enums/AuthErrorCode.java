package com.agenticcp.core.common.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증 관련 에러 코드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 인증 실패 (1001-1099)
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 1001, "로그인에 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 1002, "잘못된 사용자명 또는 비밀번호입니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, 1003, "계정이 잠겨있습니다. 잠시 후 다시 시도해주세요."),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, 1004, "비활성화된 계정입니다."),
    
    // 토큰 관련 (1100-1199)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1101, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 1102, "토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, 1103, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, 1104, "리프레시 토큰이 일치하지 않습니다."),
    TOKEN_REFRESH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 1105, "토큰 갱신에 실패했습니다."),
    
    // 로그아웃 관련 (1200-1299)
    LOGOUT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 1201, "로그아웃 처리에 실패했습니다."),
    
    // 사용자 정보 관련 (1300-1399)
    USER_INFO_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 1301, "사용자 정보 조회에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1302, "인증이 필요합니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.AUTH.generate(codeNumber);
    }
}
