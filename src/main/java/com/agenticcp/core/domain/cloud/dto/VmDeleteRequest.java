package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 가상머신(Virtual Machine) 삭제 요청을 정의하는 모델
 * AWS EC2 등 VM 종료 API를 도메인 중심으로 추상화합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmDeleteRequest {
    
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
     * 강제 삭제 여부
     * true: 인스턴스가 보호되어 있어도 강제 삭제
     * false: 보호된 인스턴스는 삭제하지 않음
     */
    @Builder.Default
    private boolean force = false;
    
    /**
     * 삭제 이유 (감사 로그용)
     * 예: "Cost optimization", "Environment cleanup", "Security incident"
     */
    private String reason;
    
    /**
     * 삭제 전 스냅샷 생성 여부
     * true: 삭제 전에 EBS 볼륨 스냅샷 생성
     * false: 스냅샷 생성하지 않음
     */
    @Builder.Default
    private boolean createSnapshot = false;
    
    /**
     * 기본 인스턴스 삭제 요청 생성
     */
    public static VmDeleteRequest basic(String instanceId) {
        return VmDeleteRequest.builder()
            .instanceId(instanceId)
            .force(false)
            .createSnapshot(false)
            .build();
    }
    
    /**
     * 강제 삭제 요청 생성
     */
    public static VmDeleteRequest force(String instanceId, String reason) {
        return VmDeleteRequest.builder()
            .instanceId(instanceId)
            .force(true)
            .reason(reason)
            .createSnapshot(false)
            .build();
    }
    
    /**
     * 스냅샷 생성 후 삭제 요청 생성
     */
    public static VmDeleteRequest withSnapshot(String instanceId, String reason) {
        return VmDeleteRequest.builder()
            .instanceId(instanceId)
            .force(false)
            .reason(reason)
            .createSnapshot(true)
            .build();
    }
    
    /**
     * 비용 최적화를 위한 삭제 요청 생성
     */
    public static VmDeleteRequest costOptimization(String instanceId) {
        return VmDeleteRequest.builder()
            .instanceId(instanceId)
            .force(false)
            .reason("Cost optimization")
            .createSnapshot(false)
            .build();
    }
    
    /**
     * 환경 정리를 위한 삭제 요청 생성
     */
    public static VmDeleteRequest environmentCleanup(String instanceId) {
        return VmDeleteRequest.builder()
            .instanceId(instanceId)
            .force(false)
            .reason("Environment cleanup")
            .createSnapshot(true)
            .build();
    }
    
    /**
     * 보안 사고로 인한 긴급 삭제 요청 생성
     */
    public static VmDeleteRequest securityIncident(String instanceId) {
        return VmDeleteRequest.builder()
            .instanceId(instanceId)
            .force(true)
            .reason("Security incident")
            .createSnapshot(true)
            .build();
    }
}
