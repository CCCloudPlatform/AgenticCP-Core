package com.agenticcp.core.domain.monitoring.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * 모니터링 도메인 에러 코드
 * 범위: 8000-8999 (MONITORING 카테고리)
 */
public enum MonitoringErrorCode implements BaseErrorCode {

    // 메트릭 수집 관련 (8001-8010)
    METRIC_COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8001, "메트릭 수집에 실패했습니다."),
    SYSTEM_METRICS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 8002, "시스템 메트릭을 사용할 수 없습니다."),
    METRIC_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8003, "메트릭 저장에 실패했습니다."),

    // 메트릭 조회 관련 (8011-8020)
    METRIC_NOT_FOUND(HttpStatus.NOT_FOUND, 8011, "메트릭을 찾을 수 없습니다."),
    INVALID_METRIC_NAME(HttpStatus.BAD_REQUEST, 8012, "유효하지 않은 메트릭 이름입니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, 8013, "유효하지 않은 시간 범위입니다."),

    // 메트릭 타입 관련 (8021-8030)
    INVALID_METRIC_TYPE(HttpStatus.BAD_REQUEST, 8021, "유효하지 않은 메트릭 타입입니다."),
    UNSUPPORTED_METRIC_TYPE(HttpStatus.BAD_REQUEST, 8022, "지원하지 않는 메트릭 타입입니다."),

    // 시스템 리소스 관련 (8031-8040)
    CPU_METRICS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 8031, "CPU 메트릭을 사용할 수 없습니다."),
    MEMORY_METRICS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 8032, "메모리 메트릭을 사용할 수 없습니다."),
    DISK_METRICS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 8033, "디스크 메트릭을 사용할 수 없습니다."),

    // 메타데이터 관련 (8041-8050)
    METADATA_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8041, "메타데이터 파싱에 실패했습니다."),
    INVALID_METADATA_FORMAT(HttpStatus.BAD_REQUEST, 8042, "유효하지 않은 메타데이터 형식입니다."),

    // 수집기 관련 (8051-8060)
    COLLECTOR_NOT_FOUND(HttpStatus.NOT_FOUND, 8051, "수집기를 찾을 수 없습니다."),
    COLLECTOR_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, 8052, "수집기가 비활성화되어 있습니다."),
    COLLECTOR_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8053, "수집기 생성에 실패했습니다."),
    INVALID_COLLECTOR_TYPE(HttpStatus.BAD_REQUEST, 8054, "유효하지 않은 수집기 타입입니다."),
    METRICS_COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8055, "메트릭 수집에 실패했습니다."),
    
    // 테넌트 수집기 설정 관련 (8061-8070)
    COLLECTOR_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 8061, "수집기 설정을 찾을 수 없습니다."),
    COLLECTOR_CONFIG_ALREADY_EXISTS(HttpStatus.CONFLICT, 8062, "이미 존재하는 수집기 설정입니다."),
    INVALID_TENANT_ID(HttpStatus.BAD_REQUEST, 8063, "유효하지 않은 테넌트 ID입니다."),
    INVALID_COLLECTION_INTERVAL(HttpStatus.BAD_REQUEST, 8064, "유효하지 않은 수집 주기입니다."),
    INVALID_RETRY_COUNT(HttpStatus.BAD_REQUEST, 8065, "유효하지 않은 재시도 횟수입니다."),
    INVALID_TIMEOUT(HttpStatus.BAD_REQUEST, 8066, "유효하지 않은 타임아웃입니다."),
    CONFIG_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8067, "설정 변환에 실패했습니다."),

    // 저장소 관련 (8071-8080)
    STORAGE_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, 8071, "저장소가 비활성화되어 있습니다."),
    STORAGE_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 8072, "저장소 연결에 실패했습니다."),
    STORAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 8073, "저장소를 찾을 수 없습니다."),
    STORAGE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8074, "저장소 생성에 실패했습니다."),
    INVALID_STORAGE_TYPE(HttpStatus.BAD_REQUEST, 8075, "유효하지 않은 저장소 타입입니다."),
    
    // 재시도 관련 (8081-8090) - Issue #39 Task 8
    RETRY_EXHAUSTED(HttpStatus.INTERNAL_SERVER_ERROR, 8081, "모든 재시도가 실패했습니다."),
    RETRY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8082, "재시도 중 오류가 발생했습니다."),
    CACHE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 8083, "캐시를 사용할 수 없습니다."),
    FALLBACK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8084, "폴백 처리에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    MonitoringErrorCode(HttpStatus httpStatus, int codeNumber, String message) {
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
        return ErrorCategory.MONITORING.generate(codeNumber);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
