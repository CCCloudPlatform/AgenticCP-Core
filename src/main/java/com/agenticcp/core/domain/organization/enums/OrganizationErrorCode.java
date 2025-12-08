package com.agenticcp.core.domain.organization.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 조직 도메인에서 사용되는 에러 코드를 정의하는 Enum 클래스입니다.
 *
 * @see BaseErrorCode
 * @see ErrorCategory
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-05
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum OrganizationErrorCode implements BaseErrorCode {

    // 테넌트 관련 (11001-11010)
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, 11001, "조직에 연결된 테넌트가 없습니다."),
    
    // 조직 관련 (11011-11020)
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, 11011, "조직을 찾을 수 없습니다."),
    ORGANIZATION_ALREADY_EXISTS(HttpStatus.CONFLICT, 11012, "이미 존재하는 조직입니다."),
    INVALID_ORGANIZATION_STATE(HttpStatus.BAD_REQUEST, 11013, "유효하지 않은 조직 상태입니다."),
    
    // 계층 구조 관련 (11021-11030)
    CIRCULAR_REFERENCE(HttpStatus.BAD_REQUEST, 11021, "순환 참조가 발생합니다."),
    HAS_CHILD_ORGANIZATIONS(HttpStatus.BAD_REQUEST, 11022, "하위 조직이 존재하는 조직은 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.ORGANIZATION.generate(codeNumber);
    }
}

