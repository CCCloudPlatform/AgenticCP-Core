package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class UpdateObjectStorageContainerCommand {

    /**
     * 프로바이더 / 계정 스코프
     */
    CloudProvider.ProviderType providerType;
    String accountScope;

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

    /**
     * 클라우드 세션 자격증명
     */
    private final CloudSessionCredential session;
}
