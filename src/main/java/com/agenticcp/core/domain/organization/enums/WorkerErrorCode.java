package com.agenticcp.core.domain.organization.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Worker 도메인에서 사용되는 에러 코드를 정의하는 Enum 클래스입니다.
 *
 * @see BaseErrorCode
 * @see ErrorCategory
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum WorkerErrorCode implements BaseErrorCode {

    // Worker 관련 (12001-12020)
    WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, 12001, "Worker를 찾을 수 없습니다."),
    WORKER_DUPLICATE_USER_TENANT(HttpStatus.CONFLICT, 12002, "같은 User와 Tenant로 이미 Worker가 생성되었습니다."),
    
    // Tenant 관련 (Worker 도메인에서 사용)
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, 12003, "테넌트를 찾을 수 없습니다."),
    
    // TenantWorkerMap 관련 (12021-12040)
    TENANT_WORKER_MAP_NOT_FOUND(HttpStatus.NOT_FOUND, 12021, "TenantWorkerMap을 찾을 수 없습니다."),
    TENANT_WORKER_MAP_ALREADY_EXISTS(HttpStatus.CONFLICT, 12022, "이미 할당된 Worker입니다."),
    TENANT_WORKER_ACCESS_DENIED(HttpStatus.FORBIDDEN, 12023, "테넌트 접근 권한이 없습니다."),
    
    // WorkerRole 관련 (12041-12060)
    WORKER_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, 12041, "WorkerRole을 찾을 수 없습니다."),
    WORKER_ROLE_ALREADY_EXISTS(HttpStatus.CONFLICT, 12042, "이미 부여된 역할입니다."),
    WORKER_ROLE_INVALID(HttpStatus.BAD_REQUEST, 12043, "유효하지 않은 역할입니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.WORKER.generate(codeNumber);
    }
}

