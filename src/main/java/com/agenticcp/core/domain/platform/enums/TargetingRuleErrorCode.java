package com.agenticcp.core.domain.platform.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.exception.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 타겟팅 규칙 관련 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * 타겟팅 규칙 생성, 수정, 삭제, 평가 과정에서 발생할 수 있는 
 * 비즈니스 예외 상황에 대한 에러 코드를 제공합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TargetingRuleErrorCode implements BaseErrorCode {

    // 타겟팅 규칙 검증 관련 에러
    RULE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, 6101, "규칙 이름은 필수입니다."),
    RULE_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, 6102, "규칙 이름은 255자를 초과할 수 없습니다."),
    RULE_NAME_TOO_SHORT(HttpStatus.BAD_REQUEST, 6103, "규칙 이름은 최소 1자 이상이어야 합니다."),
    RULE_DESCRIPTION_TOO_LONG(HttpStatus.BAD_REQUEST, 6104, "규칙 설명은 1000자를 초과할 수 없습니다."),
    
    RULE_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, 6105, "규칙 타입은 필수입니다."),
    RULE_TYPE_INVALID(HttpStatus.BAD_REQUEST, 6106, "유효하지 않은 규칙 타입입니다."),
    
    RULE_CONDITION_TOO_LONG(HttpStatus.BAD_REQUEST, 6107, "규칙 조건은 4000자를 초과할 수 없습니다."),
    RULE_CONDITION_INVALID_JSON(HttpStatus.BAD_REQUEST, 6108, "규칙 조건이 유효한 JSON 형식이 아닙니다."),
    RULE_VALUE_TOO_LONG(HttpStatus.BAD_REQUEST, 6109, "규칙 값은 4000자를 초과할 수 없습니다."),
    RULE_VALUE_INVALID_JSON(HttpStatus.BAD_REQUEST, 6110, "규칙 값이 유효한 JSON 형식이 아닙니다."),
    RULE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, 6111, "규칙 값은 필수입니다."),
    
    PRIORITY_INVALID(HttpStatus.BAD_REQUEST, 6112, "우선순위는 0 이상의 정수여야 합니다."),
    PRIORITY_TOO_HIGH(HttpStatus.BAD_REQUEST, 6113, "우선순위가 너무 높습니다. (최대 999999)"),
    
    // 타겟팅 규칙 관리 관련 에러
    TARGETING_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, 6114, "요청한 타겟팅 규칙을 찾을 수 없습니다."),
    TARGETING_RULE_ALREADY_EXISTS(HttpStatus.CONFLICT, 6115, "이미 존재하는 규칙 이름입니다."),
    TARGETING_RULE_NAME_DUPLICATE(HttpStatus.CONFLICT, 6116, "동일한 기능 플래그에 같은 이름의 규칙이 이미 존재합니다."),
    
    // 기능 플래그 관련 에러
    FEATURE_FLAG_NOT_FOUND(HttpStatus.NOT_FOUND, 6117, "요청한 기능 플래그를 찾을 수 없습니다."),
    FEATURE_FLAG_DISABLED(HttpStatus.FORBIDDEN, 6118, "비활성화된 기능 플래그입니다."),
    FEATURE_FLAG_EXPIRED(HttpStatus.FORBIDDEN, 6119, "만료된 기능 플래그입니다."),
    
    // 타겟팅 규칙 평가 관련 에러
    EVALUATION_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, 6120, "평가를 위한 사용자 ID는 필수입니다."),
    EVALUATION_TENANT_ID_REQUIRED(HttpStatus.BAD_REQUEST, 6121, "평가를 위한 테넌트 ID는 필수입니다."),
    EVALUATION_CONTEXT_INVALID(HttpStatus.BAD_REQUEST, 6122, "평가 컨텍스트가 유효하지 않습니다."),
    
    // 클라우드 프로바이더 관련 에러
    CLOUD_PROVIDER_INVALID(HttpStatus.BAD_REQUEST, 6123, "유효하지 않은 클라우드 프로바이더입니다."),
    CLOUD_REGION_INVALID(HttpStatus.BAD_REQUEST, 6124, "유효하지 않은 클라우드 리전입니다."),
    
    // 테넌트 관련 에러
    TENANT_TYPE_INVALID(HttpStatus.BAD_REQUEST, 6125, "유효하지 않은 테넌트 타입입니다."),
    TENANT_TIER_INVALID(HttpStatus.BAD_REQUEST, 6126, "유효하지 않은 테넌트 등급입니다."),
    
    // 사용자 관련 에러
    USER_ROLE_INVALID(HttpStatus.BAD_REQUEST, 6127, "유효하지 않은 사용자 역할입니다."),
    USER_ATTRIBUTE_INVALID(HttpStatus.BAD_REQUEST, 6128, "사용자 속성이 유효하지 않습니다."),
    CUSTOM_ATTRIBUTE_INVALID(HttpStatus.BAD_REQUEST, 6129, "커스텀 속성이 유효하지 않습니다."),
    
    // 롤아웃 관련 에러
    ROLLOUT_PERCENTAGE_INVALID(HttpStatus.BAD_REQUEST, 6130, "롤아웃 비율은 0-100 사이의 값이어야 합니다."),
    ROLLOUT_EVALUATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6131, "롤아웃 평가 중 오류가 발생했습니다."),
    
    // 시스템 관련 에러
    TARGETING_EVALUATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6132, "타겟팅 규칙 평가 중 오류가 발생했습니다."),
    TARGETING_RULE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6133, "타겟팅 규칙 처리 중 오류가 발생했습니다."),
    
    // 권한 관련 에러
    TARGETING_RULE_ACCESS_DENIED(HttpStatus.FORBIDDEN, 6134, "타겟팅 규칙에 대한 접근 권한이 없습니다."),
    TARGETING_RULE_MODIFICATION_DENIED(HttpStatus.FORBIDDEN, 6135, "타겟팅 규칙 수정 권한이 없습니다."),
    TARGETING_RULE_DELETION_DENIED(HttpStatus.FORBIDDEN, 6136, "타겟팅 규칙 삭제 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.PLATFORM.generate(codeNumber);
    }
}
