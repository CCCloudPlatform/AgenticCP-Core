package com.agenticcp.core.domain.tenant.dto;

import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 테넌트의 유효 설정 응답 DTO
 * 설정값과 그 출처 정보를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveConfigResponse {

    private String tenantKey;
    private Map<String, ConfigValueWithSource> configurations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigValueWithSource {
        private Object value;
        private ConfigSource source;
        private String description;
        private TenantConfig.ConfigType configType;
    }

    public enum ConfigSource {
        PLATFORM,      // 플랫폼 전역 설정
        TENANT_TYPE,   // 테넌트 타입 기본 설정
        TENANT         // 개별 테넌트 설정
    }
}

