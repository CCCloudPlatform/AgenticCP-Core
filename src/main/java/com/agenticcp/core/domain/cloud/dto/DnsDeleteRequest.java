package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DNS 호스팅 존 삭제 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsDeleteRequest {

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
     * 리전
     */
    private String region;

    /**
     * 삭제할 호스팅 존 ID
     */
    @NotBlank(message = "호스팅 존 ID는 필수입니다")
    private String zoneId;

    /**
     * 레코드가 있어도 강제 삭제 여부
     */
    @Builder.Default
    private Boolean forceDelete = false;

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
     */
    public static DnsDeleteRequest basic(String zoneId) {
        return DnsDeleteRequest.builder()
                .zoneId(zoneId)
                .forceDelete(false)
                .build();
    }
}
