package com.agenticcp.core.domain.security.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.exception.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * 보안 도메인 에러 코드
 * 
 * <p>보안 정책, 정책 엔진, 위협 탐지 등 보안 관련 비즈니스 예외를 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum SecurityErrorCode implements BaseErrorCode {
    
    // 정책 관련 에러 (5001-5020)
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "보안 정책을 찾을 수 없습니다."),
    POLICY_ALREADY_EXISTS(HttpStatus.CONFLICT, 5002, "이미 존재하는 보안 정책입니다."),
    POLICY_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 5003, "보안 정책 형식이 올바르지 않습니다."),
    POLICY_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, 5004, "보안 정책 유효성 검증에 실패했습니다."),
    POLICY_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, 5005, "이미 활성화된 보안 정책입니다."),
    POLICY_ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, 5006, "이미 비활성화된 보안 정책입니다."),
    POLICY_DELETION_FORBIDDEN(HttpStatus.FORBIDDEN, 5007, "시스템 정책은 삭제할 수 없습니다."),
    POLICY_MODIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, 5008, "시스템 정책은 수정할 수 없습니다."),
    POLICY_PRIORITY_CONFLICT(HttpStatus.CONFLICT, 5009, "정책 우선순위가 충돌합니다."),
    POLICY_EFFECTIVE_DATE_INVALID(HttpStatus.BAD_REQUEST, 5010, "정책 유효 기간이 올바르지 않습니다."),
    
    // 정책 엔진 관련 에러 (5021-5040)
    POLICY_EVALUATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5021, "정책 평가 중 오류가 발생했습니다."),
    POLICY_CONDITION_INVALID(HttpStatus.BAD_REQUEST, 5022, "정책 조건이 올바르지 않습니다."),
    POLICY_RULE_INVALID(HttpStatus.BAD_REQUEST, 5023, "정책 규칙이 올바르지 않습니다."),
    POLICY_JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, 5024, "정책 JSON 파싱에 실패했습니다."),
    POLICY_CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5025, "정책 캐시 처리 중 오류가 발생했습니다."),
    POLICY_EVALUATION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 5026, "정책 평가 시간이 초과되었습니다."),
    POLICY_DECISION_INCONCLUSIVE(HttpStatus.BAD_REQUEST, 5027, "정책 결정을 내릴 수 없습니다."),
    POLICY_CONTEXT_MISSING(HttpStatus.BAD_REQUEST, 5028, "정책 평가에 필요한 컨텍스트 정보가 누락되었습니다."),
    POLICY_RESOURCE_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, 5029, "정책과 리소스 타입이 일치하지 않습니다."),
    POLICY_ACTION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, 5030, "지원하지 않는 정책 액션입니다."),
    
    // 위협 탐지 관련 에러 (5041-5060)
    THREAT_DETECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5041, "위협 탐지 중 오류가 발생했습니다."),
    THREAT_PATTERN_INVALID(HttpStatus.BAD_REQUEST, 5042, "위협 패턴이 올바르지 않습니다."),
    THREAT_SEVERITY_INVALID(HttpStatus.BAD_REQUEST, 5043, "위협 심각도가 올바르지 않습니다."),
    THREAT_ALERT_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5044, "위협 알림 생성에 실패했습니다."),
    THREAT_FALSE_POSITIVE(HttpStatus.BAD_REQUEST, 5045, "잘못된 위협 탐지입니다."),
    THREAT_ANALYSIS_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 5046, "위협 분석 시간이 초과되었습니다."),
    THREAT_DATA_INSUFFICIENT(HttpStatus.BAD_REQUEST, 5047, "위협 분석에 필요한 데이터가 부족합니다."),
    THREAT_MODEL_OUTDATED(HttpStatus.BAD_REQUEST, 5048, "위협 탐지 모델이 오래되었습니다."),
    THREAT_QUARANTINE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5049, "위협 격리 처리에 실패했습니다."),
    THREAT_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5050, "위협 대응 처리에 실패했습니다."),
    
    // 컴플라이언스 관련 에러 (5061-5080)
    COMPLIANCE_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5061, "컴플라이언스 검사에 실패했습니다."),
    COMPLIANCE_RULE_VIOLATION(HttpStatus.BAD_REQUEST, 5062, "컴플라이언스 규칙을 위반했습니다."),
    COMPLIANCE_AUDIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5063, "컴플라이언스 감사에 실패했습니다."),
    COMPLIANCE_REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5064, "컴플라이언스 보고서 생성에 실패했습니다."),
    COMPLIANCE_STANDARD_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, 5065, "지원하지 않는 컴플라이언스 표준입니다."),
    COMPLIANCE_REQUIREMENT_MISSING(HttpStatus.BAD_REQUEST, 5066, "컴플라이언스 요구사항이 누락되었습니다."),
    COMPLIANCE_EVIDENCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, 5067, "컴플라이언스 증거가 부족합니다."),
    COMPLIANCE_CERTIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, 5068, "컴플라이언스 인증이 만료되었습니다."),
    COMPLIANCE_REMEDIATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5069, "컴플라이언스 개선 조치에 실패했습니다."),
    COMPLIANCE_MONITORING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5070, "컴플라이언스 모니터링에 실패했습니다."),
    
    // 보안 설정 관련 에러 (5081-5100)
    SECURITY_CONFIG_INVALID(HttpStatus.BAD_REQUEST, 5081, "보안 설정이 올바르지 않습니다."),
    SECURITY_CONFIG_MISSING(HttpStatus.BAD_REQUEST, 5082, "필수 보안 설정이 누락되었습니다."),
    SECURITY_CONFIG_CONFLICT(HttpStatus.CONFLICT, 5083, "보안 설정이 충돌합니다."),
    SECURITY_CONFIG_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5084, "보안 설정 업데이트에 실패했습니다."),
    SECURITY_CONFIG_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, 5085, "보안 설정 유효성 검증에 실패했습니다."),
    SECURITY_CONFIG_BACKUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5086, "보안 설정 백업에 실패했습니다."),
    SECURITY_CONFIG_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5087, "보안 설정 복원에 실패했습니다."),
    SECURITY_CONFIG_EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5088, "보안 설정 내보내기에 실패했습니다."),
    SECURITY_CONFIG_IMPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5089, "보안 설정 가져오기에 실패했습니다."),
    SECURITY_CONFIG_VERSION_MISMATCH(HttpStatus.BAD_REQUEST, 5090, "보안 설정 버전이 일치하지 않습니다."),
    
    // 보안 로그 관련 에러 (5101-5120)
    SECURITY_LOG_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5101, "보안 로그 생성에 실패했습니다."),
    SECURITY_LOG_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5102, "보안 로그 조회에 실패했습니다."),
    SECURITY_LOG_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5103, "보안 로그 분석에 실패했습니다."),
    SECURITY_LOG_RETENTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5104, "보안 로그 보존 처리에 실패했습니다."),
    SECURITY_LOG_ARCHIVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5105, "보안 로그 아카이브에 실패했습니다."),
    SECURITY_LOG_CORRUPTED(HttpStatus.BAD_REQUEST, 5106, "보안 로그가 손상되었습니다."),
    SECURITY_LOG_FORMAT_INVALID(HttpStatus.BAD_REQUEST, 5107, "보안 로그 형식이 올바르지 않습니다."),
    SECURITY_LOG_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, 5108, "보안 로그 크기가 제한을 초과했습니다."),
    SECURITY_LOG_ACCESS_DENIED(HttpStatus.FORBIDDEN, 5109, "보안 로그 접근이 거부되었습니다."),
    SECURITY_LOG_INTEGRITY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5110, "보안 로그 무결성 검증에 실패했습니다.");
    
    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    SecurityErrorCode(HttpStatus httpStatus, int codeNumber, String message) {
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
        return ErrorCategory.SECURITY.generate(codeNumber);
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}
