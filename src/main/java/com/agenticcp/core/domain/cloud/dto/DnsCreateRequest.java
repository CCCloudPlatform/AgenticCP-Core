package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DNS 호스팅 존 생성 요청 DTO
 * 
 * Controller에서 받는 요청 객체로, CSP 중립적인 필드만 포함합니다.
 * CSP 특화 설정은 providerSpecificConfig에 포함됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsCreateRequest {

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
     * Route53은 글로벌 서비스이지만 일관성을 위해 리전 파라미터는 유지
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 호스팅 존 이름 (필수)
     * CSP 중립적: example.com
     */
    @NotBlank(message = "호스팅 존 이름은 필수입니다")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9\\-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9\\-]{0,61}[a-z0-9])?)*$",
            message = "유효한 도메인 이름 형식이 아닙니다")
    private String zoneName;

    /**
     * 존 타입 (필수)
     * CSP 중립적: "PUBLIC", "PRIVATE"
     */
    @NotBlank(message = "존 타입은 필수입니다")
    @Pattern(regexp = "PUBLIC|PRIVATE", message = "PUBLIC 또는 PRIVATE이어야 합니다")
    private String zoneType;

    /**
     * VPC ID (Private Zone인 경우 선택적)
     */
    private String vpcId;

    /**
     * 호스팅 존 설명
     */
    private String comment;

    /**
     * 태그
     */
    private Map<String, String> tags;

    /**
     * CSP별 특화 설정
     * 
     * AWS 예시:
     *   - delegationSetId: "N1234567890" (재사용 가능한 위임 집합 ID)
     * 
     * Azure 예시:
     *   - resourceGroupName: "my-resource-group"
     *   - zoneType: "Public" | "Private"
     *   - registrationVirtualNetworkIds: ["vnet-id-1", "vnet-id-2"]
     * 
     * GCP 예시:
     *   - description: "Managed zone description"
     *   - dnsName: "example.com."
     *   - visibility: "public" | "private"
     *   - privateVisibilityConfig: { networks: [...] }
     */
    private Map<String, Object> providerSpecificConfig;
}
