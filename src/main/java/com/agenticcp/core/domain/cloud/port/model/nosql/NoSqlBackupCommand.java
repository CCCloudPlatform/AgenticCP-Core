package com.agenticcp.core.domain.cloud.port.model.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * NoSQL 백업/복원 관련 도메인 커맨드
 *
 * 단일 모델로 create/list/delete/restore 요청을 표현하고,
 * operationType 에 따라 어댑터에서 분기 처리합니다.
 */
@Getter
@Builder
public class NoSqlBackupCommand {

    public enum OperationType {
        CREATE_BACKUP,
        DELETE_BACKUP,
        LIST_BACKUPS,
        RESTORE_FROM_BACKUP,
        RESTORE_TO_POINT_IN_TIME
    }

    private final CloudProvider.ProviderType providerType;
    private final String accountScope;

    /**
     * 대상 테이블 이름
     */
    private final String tableName;

    /**
     * 백업 ID (삭제/복원 등에 사용)
     */
    private final String backupId;

    /**
     * 복원 대상 새 테이블 이름 (선택)
     */
    private final String targetTableName;

    /**
     * 시점 복원 시 기준 시각
     */
    private final Instant restorePointInTime;

    /**
     * 백업 검색/정리를 위한 기간 설정 (선택)
     */
    private final Instant fromTime;
    private final Instant toTime;

    /**
     * 수행할 작업 타입
     */
    private final OperationType operationType;

    /**
     * 세션 자격증명
     */
    private final CloudSessionCredential session;
}


