package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Object Storage Container 업데이트 명령
 * 어댑터 레이어에서 사용하는 내부 명령 객체
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Getter
@Builder
public class UpdateObjectStorageContainerCommand {

    /**
     * 클라우드 프로바이더 타입
     */
    CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프
     */
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
