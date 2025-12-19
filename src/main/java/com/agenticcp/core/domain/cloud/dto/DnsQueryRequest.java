package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * DNS 호스팅 존 조회 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsQueryRequest {

    /**
     * 클라우드 프로바이더 타입
     * Controller에서 PathVariable로 주입됩니다.
     */
    @NotNull
    private ProviderType providerType;

    /**
     * 계정 스코프
     * Controller에서 PathVariable로 주입됩니다.
     */
    @NotBlank
    private String accountScope;

    /**
     * 조회할 리전 목록
     * Route53은 글로벌 서비스이므로 무시될 수 있음
     */
    private Set<String> regions;

    /**
     * 호스팅 존 이름으로 필터링
     */
    private String zoneName;

    /**
     * "PUBLIC", "PRIVATE"로 필터링
     */
    private String zoneType;

    /**
     * Private Zone인 경우 VPC ID로 필터링
     */
    private String vpcId;

    /**
     * 태그로 필터링
     */
    private Map<String, String> tags;

    /**
     * 페이지 번호 (0부터 시작)
     */
    @Min(0)
    @Builder.Default
    private int page = 0;

    /**
     * 페이지 크기
     */
    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 20;
}
