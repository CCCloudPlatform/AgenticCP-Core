package com.agenticcp.core.domain.user.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 역할(Role) 도메인 관련 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * 역할 관리 관련 비즈니스 로직에서 발생하는 예외 상황들을 정의합니다.
 * 에러 코드 범위: 2000-2999 (USER 도메인 내)
 * </p>
 *
 * @see BaseErrorCode
 * @see ErrorCategory
 * @author AgenticCP Team
 * @since 2025-09-22
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum RoleErrorCode implements BaseErrorCode {

    // 역할 조회 관련 (2041-2050)
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, 2041, "역할을 찾을 수 없습니다."),
    ROLE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 2042, "이미 삭제된 역할입니다."),
    
    // 역할 생성/수정 관련 (2051-2060)
    DUPLICATE_ROLE_KEY(HttpStatus.CONFLICT, 2051, "이미 존재하는 역할 키입니다."),
    DUPLICATE_ROLE_NAME(HttpStatus.CONFLICT, 2052, "이미 존재하는 역할명입니다."),
    INVALID_ROLE_STATUS(HttpStatus.BAD_REQUEST, 2053, "유효하지 않은 역할 상태입니다."),
    
    // 시스템 역할 보호 관련 (2061-2070)
    SYSTEM_ROLE_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, 2061, "시스템 역할은 수정할 수 없습니다."),
    SYSTEM_ROLE_NOT_DELETABLE(HttpStatus.BAD_REQUEST, 2062, "시스템 역할은 삭제할 수 없습니다."),
    DEFAULT_ROLE_NOT_DELETABLE(HttpStatus.BAD_REQUEST, 2063, "기본 역할은 삭제할 수 없습니다."),
    
    // 역할 사용 관련 (2071-2080)
    ROLE_IN_USE(HttpStatus.BAD_REQUEST, 2071, "이 역할을 사용하는 사용자가 있어 삭제할 수 없습니다."),
    ROLE_ASSIGNMENT_FAILED(HttpStatus.BAD_REQUEST, 2072, "역할 할당에 실패했습니다."),
    
    // 권한 매핑 관련 (2081-2090)
    INVALID_TENANT_ACCESS(HttpStatus.FORBIDDEN, 2081, "다른 테넌트의 역할에 접근할 수 없습니다."),
    PERMISSION_NOT_FOUND(HttpStatus.BAD_REQUEST, 2082, "일부 권한을 찾을 수 없습니다."),
    INVALID_TENANT_PERMISSION_ACCESS(HttpStatus.FORBIDDEN, 2083, "다른 테넌트의 권한에 접근할 수 없습니다."),
    
    // 역할 우선순위 관련 (2091-2100)
    INVALID_ROLE_PRIORITY(HttpStatus.BAD_REQUEST, 2091, "유효하지 않은 역할 우선순위입니다."),
    ROLE_PRIORITY_CONFLICT(HttpStatus.CONFLICT, 2092, "역할 우선순위가 충돌합니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.USER.generate(codeNumber);
    }
}
