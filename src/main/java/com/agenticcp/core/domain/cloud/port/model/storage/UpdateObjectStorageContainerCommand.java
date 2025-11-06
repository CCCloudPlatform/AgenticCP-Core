package com.agenticcp.core.domain.cloud.port.model.storage;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class UpdateObjectStorageContainerCommand {

/**
 * 업데이트할 Container 이름 (필수)
 */
private final String containerName;

    /**
     * 버전 관리 활성화 여부 (선택적)
     */
    private final Boolean versioningEnabled;

    /**
     * 태그 (선택적)
     * - null: 태그를 변경하지 않음
     * - empty map: 모든 태그 제거
     * - map with entries: 태그 덮어쓰기
     */
    private final Map<String, String> tags;
}
