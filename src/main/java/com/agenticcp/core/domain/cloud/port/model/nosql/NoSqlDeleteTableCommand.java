package com.agenticcp.core.domain.cloud.port.model.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

/**
 * NoSQL 테이블 삭제 도메인 커맨드 (CSP 중립적)
 */
@Getter
@Builder
public class NoSqlDeleteTableCommand {

    private final CloudProvider.ProviderType providerType;
    private final String accountScope;

    /**
     * 삭제 대상 테이블 이름
     */
    private final String tableName;

    /**
     * 리전/위치 (선택)
     */
    private final String region;

    /**
     * 강제 삭제 여부 (CSP에 따라 의미가 다를 수 있음)
     */
    private final boolean force;

    /**
     * 삭제 사유 (감사/로그용)
     */
    private final String reason;

    /**
     * 세션 자격증명
     */
    private final CloudSessionCredential session;
}


