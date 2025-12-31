package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * RDBMS 삭제 요청 DTO (CSP 중립적)
 * 
 * RDBMS 인스턴스의 삭제를 위한 요청 객체입니다.
 * CSP 중립적인 필드를 사용하며, CSP 특화 삭제 옵션은 providerSpecificConfig에 포함됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RdbmsDeleteRequest {
    
    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;
    
    /**
     * 삭제할 인스턴스 ID
     */
    private String instanceId;
    
    /**
     * 최종 스냅샷 건너뛰기
     * true인 경우 최종 스냅샷을 생성하지 않고 삭제합니다.
     * 기본값: false (스냅샷 생성)
     */
    @Builder.Default
    private Boolean skipSnapshot = false;
    
    /**
     * 최종 스냅샷 이름 (선택적)
     * skipSnapshot이 false인 경우 사용됩니다.
     */
    private String snapshotName;
    
    /**
     * 자동 백업 삭제 여부
     * true인 경우 자동 백업도 함께 삭제합니다.
     * 기본값: false (자동 백업 유지)
     */
    @Builder.Default
    private Boolean deleteAutomatedBackups = false;
    
    /**
     * 삭제 이유 (감사 로그용)
     */
    private String reason;
    
    /**
     * CSP별 특화 삭제 옵션
     */
    private Map<String, Object> providerSpecificConfig;
    
    /**
     * 기본 삭제 요청 생성
     * 최종 스냅샷을 생성하고 자동 백업은 유지합니다.
     * 
     * @param instanceId 삭제할 인스턴스 ID
     * @return 기본 삭제 요청
     */
    public static RdbmsDeleteRequest basic(String instanceId) {
        return RdbmsDeleteRequest.builder()
            .instanceId(instanceId)
            .skipSnapshot(false)
            .deleteAutomatedBackups(false)
            .build();
    }
    
    /**
     * 강제 삭제 요청 생성
     * 최종 스냅샷을 건너뛰고 자동 백업도 삭제합니다.
     * 
     * @param instanceId 삭제할 인스턴스 ID
     * @return 강제 삭제 요청
     */
    public static RdbmsDeleteRequest force(String instanceId) {
        return RdbmsDeleteRequest.builder()
            .instanceId(instanceId)
            .skipSnapshot(true)
            .deleteAutomatedBackups(true)
            .build();
    }
}
