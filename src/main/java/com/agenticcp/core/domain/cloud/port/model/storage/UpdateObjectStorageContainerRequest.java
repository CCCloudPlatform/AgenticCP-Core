package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Object Storage Container 업데이트 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateObjectStorageContainerRequest {

    /**
     * 클라우드 프로바이더 타입
     */
    CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프
     */
    String accountScope;

    /**
     * 업데이트할 Container 이름
     */
    String containerName;
    
    /**
     * 버전 관리 활성화 여부 (선택적)
     * - 객체의 여러 버전을 유지할지 여부
     */
    private Boolean versioningEnabled;
    
    /**
     * 태그 (선택적)
     * - Container 분류 및 관리를 위한 키-값 쌍
     * - 기존 태그를 업데이트하거나 새로 추가
     */
    private Map<String, String> tags;

}
