package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 클라우드 계정 메타데이터 모델
 * 
 * 외부 클라우드 API에서 조회한 계정 상세 정보입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountMetadata {
    
    /**
     * 계정 ID
     */
    private String accountId;
    
    /**
     * 프로바이더 타입
     */
    private ProviderType providerType;
    
    /**
     * 계정명 (CSP에서 제공하는 이름)
     */
    private String accountName;
    
    /**
     * 계정 소유자 이메일/이름
     */
    private String accountOwner;
    
    /**
     * 지원 가능한 리전/존/로케이션 목록
     */
    private List<String> availableRegions;
    
    /**
     * 기본 리전
     */
    private String defaultRegion;
    
    /**
     * 계정 상태 (CSP에서 제공하는 상태)
     */
    private String accountStatus;
    
    /**
     * AWS 전용: Organization ID
     */
    private String organizationId;
    
    /**
     * GCP 전용: Project Number
     */
    private String projectNumber;
    
    /**
     * Azure 전용: Subscription Name
     */
    private String subscriptionName;
    
    /**
     * 활성화된 서비스 목록
     */
    private List<String> enabledServices;
    
    /**
     * 추가 메타데이터 (CSP별 확장 정보)
     */
    private Map<String, Object> customMetadata;
}

