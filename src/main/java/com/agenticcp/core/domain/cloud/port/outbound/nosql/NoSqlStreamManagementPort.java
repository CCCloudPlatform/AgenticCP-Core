package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;

/**
 * NoSQL 스트림 관리 포트
 *
 * 테이블 스트림(변경 이벤트 스트림)의 활성화/비활성화 및 구성 변경을 담당합니다.
 * 스트림 설정은 보통 테이블 설정의 일부로 취급되므로,
 * NoSqlUpdateTableCommand 를 재사용합니다.
 */
public interface NoSqlStreamManagementPort {

    /**
     * 스트림 구성을 변경합니다.
     *
     * @param command 스트림 설정 변경을 포함하는 업데이트 명령
     * @return 변경 후 테이블 리소스 표현
     */
    CloudResource updateStreamConfiguration(NoSqlUpdateTableCommand command);
}


