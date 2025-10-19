package com.agenticcp.core.domain.ui.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 메뉴 도메인 에러 코드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements BaseErrorCode {

    // 10000-10999: 메뉴 도메인
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, 10001, "메뉴를 찾을 수 없습니다."),
    DUPLICATE_MENU_KEY(HttpStatus.CONFLICT, 10002, "이미 사용 중인 메뉴 키입니다."),
    MENU_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 10003, "이미 삭제된 메뉴입니다."),
    SYSTEM_MENU_CANNOT_DELETE(HttpStatus.FORBIDDEN, 10004, "시스템 메뉴는 삭제할 수 없습니다."),
    INVALID_MENU_HIERARCHY(HttpStatus.BAD_REQUEST, 10005, "유효하지 않은 메뉴 계층 구조입니다."),
    MENU_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, 10006, "메뉴 깊이가 최대 허용 깊이(5단계)를 초과했습니다."),
    CANNOT_DELETE_MENU_WITH_CHILDREN(HttpStatus.BAD_REQUEST, 10007, "하위 메뉴가 있는 메뉴는 삭제할 수 없습니다."),
    INVALID_PARENT_MENU(HttpStatus.BAD_REQUEST, 10008, "유효하지 않은 부모 메뉴입니다."),
    MENU_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, 10009, "메뉴 권한 매핑을 찾을 수 없습니다."),
    DUPLICATE_MENU_PERMISSION(HttpStatus.CONFLICT, 10010, "이미 설정된 메뉴 권한입니다."),
    INVALID_ACCESS_TYPE(HttpStatus.BAD_REQUEST, 10011, "유효하지 않은 접근 타입입니다."),
    MENU_ACCESS_DENIED(HttpStatus.FORBIDDEN, 10012, "메뉴 접근 권한이 없습니다."),
    MENU_CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 10013, "메뉴 캐시 처리 중 오류가 발생했습니다."),
    MENU_STRUCTURE_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, 10014, "메뉴 구조 검증에 실패했습니다."),
    MENU_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 10015, "메뉴 초기화에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.UI.generate(codeNumber);  // "UI_9001"
    }
}
