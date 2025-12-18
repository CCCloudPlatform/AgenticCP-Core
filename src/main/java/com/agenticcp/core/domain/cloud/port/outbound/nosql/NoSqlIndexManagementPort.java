package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;

/**
 * NoSQL 인덱스 관리 포트
 *
 * 글로벌 보조 인덱스(GSI) / 세컨더리 인덱스의 생성, 수정, 삭제를 담당합니다.
 * 일부 CSP는 테이블 업데이트 API를 통해 인덱스를 관리하므로,
 * Command 수준에서는 NoSqlUpdateTableCommand를 재사용할 수 있습니다.
 */
public interface NoSqlIndexManagementPort {

    /**
     * 인덱스 구성을 변경합니다.
     *
     * @param command 인덱스 변경을 포함하는 업데이트 명령
     * @return 변경 후 테이블 리소스 표현
     */
    CloudResource updateIndexes(NoSqlUpdateTableCommand command);
}


