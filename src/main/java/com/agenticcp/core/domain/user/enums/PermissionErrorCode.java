package com.agenticcp.core.domain.user.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 권한(Permission) 도메인 관련 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * 권한 관리 관련 비즈니스 로직에서 발생하는 예외 상황들을 정의합니다.
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
public enum PermissionErrorCode implements BaseErrorCode {

    // 권한 조회 관련 (2101-2110)
    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, 2101, "권한을 찾을 수 없습니다."),
    PERMISSION_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 2102, "이미 삭제된 권한입니다."),
    
    // 권한 생성/수정 관련 (2111-2120)
    DUPLICATE_PERMISSION_KEY(HttpStatus.CONFLICT, 2111, "이미 존재하는 권한 키입니다."),
    DUPLICATE_PERMISSION_NAME(HttpStatus.CONFLICT, 2112, "이미 존재하는 권한명입니다."),
    INVALID_PERMISSION_STATUS(HttpStatus.BAD_REQUEST, 2113, "유효하지 않은 권한 상태입니다."),
    INVALID_PERMISSION_FORMAT(HttpStatus.BAD_REQUEST, 2114, "권한 키 형식이 올바르지 않습니다."),
    
    // 시스템 권한 보호 관련 (2121-2130)
    SYSTEM_PERMISSION_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, 2121, "시스템 권한은 수정할 수 없습니다."),
    SYSTEM_PERMISSION_NOT_DELETABLE(HttpStatus.BAD_REQUEST, 2122, "시스템 권한은 삭제할 수 없습니다."),
    
    // 권한 사용 관련 (2131-2140)
    PERMISSION_IN_USE(HttpStatus.BAD_REQUEST, 2131, "이 권한을 사용하는 역할이 있어 삭제할 수 없습니다."),
    PERMISSION_ASSIGNMENT_FAILED(HttpStatus.BAD_REQUEST, 2132, "권한 할당에 실패했습니다."),
    
    // 권한 매핑 관련 (2141-2150)
    INVALID_TENANT_ACCESS(HttpStatus.FORBIDDEN, 2141, "다른 테넌트의 권한에 접근할 수 없습니다."),
    INVALID_RESOURCE_ACTION(HttpStatus.BAD_REQUEST, 2142, "유효하지 않은 리소스 또는 액션입니다."),
    
    // 권한 우선순위 관련 (2151-2160)
    INVALID_PERMISSION_PRIORITY(HttpStatus.BAD_REQUEST, 2151, "유효하지 않은 권한 우선순위입니다."),
    PERMISSION_PRIORITY_CONFLICT(HttpStatus.CONFLICT, 2152, "권한 우선순위가 충돌합니다."),
    
    // 권한 검증 관련 (2161-2170)
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, 2161, "해당 권한이 없습니다."),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, 2162, "권한이 부족합니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.USER.generate(codeNumber);
    }
}
