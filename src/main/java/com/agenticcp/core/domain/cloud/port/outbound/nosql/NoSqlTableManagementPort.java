package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlCreateTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlDeleteTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;

/**
 * NoSQL 테이블 관리 포트
 *
 * 테이블 생성/수정/삭제 등 테이블 수준의 생명주기 관리를 담당합니다.
 * VM / Object Storage와 동일하게 헥사고날 아키텍처의 Outbound Port 계층에 속합니다.
 */
public interface NoSqlTableManagementPort {

    /**
     * NoSQL 테이블을 생성합니다.
     *
     * @param command 생성 명령 (세션 포함)
     * @return 생성된 클라우드 리소스 표현
     */
    CloudResource createTable(NoSqlCreateTableCommand command);

    /**
     * NoSQL 테이블 구성을 수정합니다.
     *
     * @param command 수정 명령 (세션 포함)
     * @return 수정된 클라우드 리소스 표현
     */
    CloudResource updateTable(NoSqlUpdateTableCommand command);

    /**
     * NoSQL 테이블을 삭제합니다.
     *
     * @param command 삭제 명령 (세션 포함)
     */
    void deleteTable(NoSqlDeleteTableCommand command);
}


