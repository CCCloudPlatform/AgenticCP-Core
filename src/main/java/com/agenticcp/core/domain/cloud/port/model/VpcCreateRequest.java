package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * VPC 생성 요청 모델
 * 
 * 모든 클라우드 프로바이더에서 VPC 생성을 위한 표준화된 요청 모델
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class VpcCreateRequest {
    
    /**
     * 클라우드 프로바이더 타입
     */
    ProviderType providerType;
    
    /**
     * 계정 범위
     * - AWS: Account ID
     * - Azure: Subscription ID  
     * - GCP: Project ID
     */
    String accountScope;
    
    /**
     * 리전
     * - AWS: us-east-1, ap-northeast-2
     * - Azure: koreacentral, eastus
     * - GCP: asia-northeast1, us-central1
     */
    String region;
    
    /**
     * VPC 이름
     */
    String vpcName;
    
    /**
     * CIDR 블록
     * 예: 10.0.0.0/16
     */
    String cidrBlock;
    
    /**
     * VPC 설명 (선택사항)
     */
    String description;
    
    /**
     * 태그 정보 (선택사항)
     */
    Map<String, String> tags;
    
    /**
     * 테넌트 키
     */
    String tenantKey;
    
    /**
     * 프로바이더별 특화 설정
     * - AWS: instanceTenancy, ipv6CidrBlock 등
     * - GCP: autoCreateSubnetworks, routingMode 등
     * - Azure: enableDdosProtection, enableVmProtection 등
     */
    Map<String, Object> providerSpecificConfig;
}