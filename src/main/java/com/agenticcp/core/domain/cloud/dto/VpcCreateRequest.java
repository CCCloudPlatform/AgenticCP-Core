package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * VPC 생성 요청 모델
 * 
 * 모든 클라우드 프로바이더에서 VPC 생성을 위한 표준화된 요청 모델
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpcCreateRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * 리전 (필수)
     * 예: us-east-1, ap-northeast-2
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * VPC 이름
     */
    private String vpcName;

    /**
     * CIDR 블록 (필수)
     * 예: 10.0.0.0/16
     */
    @NotBlank(message = "CIDR 블록은 필수입니다")
    @Pattern(regexp = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,2}$",
            message = "올바른 CIDR 형식이어야 합니다 (예: 10.0.0.0/16)")
    private String cidrBlock;

    /**
     * VPC 설명
     */
    private String description;

    /**
     * 태그
     */
    private Map<String, String> tags;

    /**
     * 테넌트 키
     */
    private String tenantKey;

    /**
     * 프로바이더별 추가 설정
     */
    private Map<String, Object> providerSpecificConfig;
}
