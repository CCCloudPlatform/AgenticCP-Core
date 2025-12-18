package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlBackupCommand;
import org.springframework.data.domain.Page;

/**
 * NoSQL 백업/복원 포트
 *
 * 온디맨드 백업 생성, 목록 조회, 삭제, 복원 작업을 담당합니다.
 */
public interface NoSqlBackupPort {

    /**
     * 백업 관련 작업을 수행합니다.
     *
     * OperationType 에 따라 아래와 같이 동작합니다.
     * - CREATE_BACKUP: 새 백업 생성 후 백업 정보를 반환
     * - DELETE_BACKUP: 백업 삭제 (void 반환 가능)
     * - LIST_BACKUPS: 페이징된 백업 목록 반환
     * - RESTORE_FROM_BACKUP / RESTORE_TO_POINT_IN_TIME: 복원된 테이블 리소스를 반환
     *
     * 구현체는 OperationType 에 따라 적절한 반환 타입을 선택하거나,
     * 필요 시 별도 메서드로 분리할 수 있습니다.
     */
    default Object execute(NoSqlBackupCommand command) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * 명시적인 API가 필요한 경우를 위해 대표적인 연산들을 분리해 둡니다.
     */
    CloudResource createBackup(NoSqlBackupCommand command);

    Page<CloudResource> listBackups(NoSqlBackupCommand command);

    void deleteBackup(NoSqlBackupCommand command);

    CloudResource restore(NoSqlBackupCommand command);
}


