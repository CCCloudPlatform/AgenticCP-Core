package com.agenticcp.core.domain.platform.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 기능 플래그 캐시 관련 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * Redis 캐시, 분산 락, 캐시 동기화 과정에서 발생할 수 있는
 * 비즈니스 예외 상황에 대한 에러 코드를 제공합니다.
 * </p>
 * <p>
 * 에러 코드 범위: 6200-6299 (캐시 전용)
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-10-17
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum FeatureFlagCacheErrorCode implements BaseErrorCode {

    // 캐시 가용성 관련 에러 (6201-6210)
    CACHE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 6201, "Redis 캐시를 사용할 수 없습니다."),
    CACHE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6202, "캐시 작업이 실패했습니다."),

    // 분산 락 관련 에러 (6211-6220)
    DISTRIBUTED_LOCK_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 6203, "분산 락 획득 시간이 초과되었습니다."),
    DISTRIBUTED_LOCK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6204, "분산 락 획득에 실패했습니다."),

    // 캐시 동기화 관련 에러 (6221-6230)
    CACHE_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6205, "캐시 동기화가 실패했습니다."),

    // 캐시 키 관련 에러 (6231-6240)
    INVALID_CACHE_KEY(HttpStatus.BAD_REQUEST, 6206, "유효하지 않은 캐시 키입니다."),

    // 캐시 관리 관련 에러 (6241-6250)
    CACHE_WARMUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6207, "캐시 Warm-up에 실패했습니다."),
    CACHE_INVALIDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6208, "캐시 무효화에 실패했습니다."),

    // TTL 관련 에러 (6251-6260)
    INVALID_CACHE_TTL(HttpStatus.BAD_REQUEST, 6209, "유효하지 않은 TTL 값입니다. (10~3600초 범위)");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.PLATFORM.generate(codeNumber);
    }
}

