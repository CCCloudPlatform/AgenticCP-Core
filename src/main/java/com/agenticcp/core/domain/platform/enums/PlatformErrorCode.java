package com.agenticcp.core.domain.platform.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * 플랫폼 도메인 에러 코드
 * 
 * 플랫폼 관리 관련 비즈니스 예외를 정의합니다.
 * 에러 코드 번호 범위: 6000-6999
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public enum PlatformErrorCode implements BaseErrorCode {
    
    // 기능 플래그 관련 (6001-6050)
    FEATURE_FLAG_NOT_FOUND(HttpStatus.NOT_FOUND, 6001, "기능 플래그를 찾을 수 없습니다."),
    FEATURE_FLAG_DUPLICATE_KEY(HttpStatus.CONFLICT, 6002, "이미 사용 중인 기능 플래그 키입니다."),
    FEATURE_FLAG_INVALID_STATE(HttpStatus.BAD_REQUEST, 6003, "잘못된 기능 플래그 상태입니다."),
    FEATURE_FLAG_SYSTEM_FLAG_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, 6004, "시스템 플래그는 삭제할 수 없습니다."),
    
    // 타겟팅 규칙 관련 (6051-6100)
    TARGET_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, 6051, "타겟팅 규칙을 찾을 수 없습니다."),
    TARGET_RULE_DUPLICATE_NAME(HttpStatus.CONFLICT, 6052, "해당 기능 플래그에 동일한 규칙명이 이미 존재합니다."),
    TARGET_RULE_INVALID_ROLLOUT_PERCENTAGE(HttpStatus.BAD_REQUEST, 6053, "롤아웃 비율은 0-100% 범위 내에서만 설정 가능합니다."),
    TARGET_RULE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, 6054, "시작일은 종료일보다 이전이어야 합니다."),
    TARGET_RULE_INVALID_PRIORITY(HttpStatus.BAD_REQUEST, 6055, "우선순위는 0-999 범위 내에서만 설정 가능합니다."),
    TARGET_RULE_INVALID_STRATEGY(HttpStatus.BAD_REQUEST, 6056, "지원하지 않는 롤아웃 전략입니다."),
    TARGET_RULE_EXPIRED(HttpStatus.BAD_REQUEST, 6057, "만료된 타겟팅 규칙입니다."),
    TARGET_RULE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, 6058, "비활성 상태의 타겟팅 규칙입니다."),
    
    // 플랫폼 설정 관련 (6101-6150)
    PLATFORM_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 6101, "플랫폼 설정을 찾을 수 없습니다."),
    PLATFORM_CONFIG_DUPLICATE_KEY(HttpStatus.CONFLICT, 6102, "이미 사용 중인 설정 키입니다."),
    PLATFORM_CONFIG_INVALID_VALUE(HttpStatus.BAD_REQUEST, 6103, "잘못된 설정 값입니다."),
    PLATFORM_CONFIG_SYSTEM_CONFIG_MODIFY_FORBIDDEN(HttpStatus.FORBIDDEN, 6104, "시스템 설정은 수정할 수 없습니다."),
    
    // 라이선스 관련 (6151-6200)
    LICENSE_NOT_FOUND(HttpStatus.NOT_FOUND, 6151, "라이선스를 찾을 수 없습니다."),
    LICENSE_EXPIRED(HttpStatus.BAD_REQUEST, 6152, "만료된 라이선스입니다."),
    LICENSE_INVALID(HttpStatus.BAD_REQUEST, 6153, "유효하지 않은 라이선스입니다."),
    LICENSE_EXCEEDED_LIMIT(HttpStatus.BAD_REQUEST, 6154, "라이선스 한도를 초과했습니다."),
    
    // 플랫폼 상태 관련 (6201-6250)
    PLATFORM_HEALTH_CHECK_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 6201, "플랫폼 상태 확인에 실패했습니다."),
    PLATFORM_MAINTENANCE_MODE(HttpStatus.SERVICE_UNAVAILABLE, 6202, "플랫폼이 유지보수 모드입니다."),
    PLATFORM_OVERLOADED(HttpStatus.SERVICE_UNAVAILABLE, 6203, "플랫폼이 과부하 상태입니다."),
    
    // 일반적인 플랫폼 오류 (6251-6299)
    PLATFORM_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6251, "플랫폼 작업이 실패했습니다."),
    PLATFORM_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6252, "플랫폼 설정 오류가 발생했습니다."),
    PLATFORM_INTEGRATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6253, "플랫폼 통합 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    PlatformErrorCode(HttpStatus httpStatus, int codeNumber, String message) {
        this.httpStatus = httpStatus;
        this.codeNumber = codeNumber;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return ErrorCategory.PLATFORM.generate(codeNumber);
    }

    @Override
    public String getMessage() {
        return message;
    }

}
