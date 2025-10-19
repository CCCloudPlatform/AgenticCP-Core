package com.agenticcp.core.domain.platform.cache.dto;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 기능 플래그 캐시용 DTO
 * Redis에 저장할 플래그 설정 정보
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "기능 플래그 캐시 DTO")
public class FeatureFlagCacheDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "플래그 ID", example = "1")
    private Long id;

    @Schema(description = "플래그 키", example = "new-feature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String flagKey;

    @Schema(description = "플래그 이름", example = "새로운 기능", requiredMode = Schema.RequiredMode.REQUIRED)
    private String flagName;

    @Schema(description = "설명", example = "새로운 기능에 대한 설명")
    private String description;

    @Schema(description = "활성화 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isEnabled;

    @Schema(description = "상태", example = "ACTIVE")
    private Status status;

    @Schema(description = "대상 테넌트 목록 (JSON)", example = "[\"tenant1\", \"tenant2\"]")
    private String targetTenants;

    @Schema(description = "대상 사용자 목록 (JSON)", example = "[\"user1\", \"user2\"]")
    private String targetUsers;

    @Schema(description = "롤아웃 비율 (%)", example = "50")
    private Integer rolloutPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "시작 일시", example = "2025-01-01T00:00:00")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "종료 일시", example = "2025-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "메타데이터 (JSON)")
    private String metadata;

    @Schema(description = "캐시 TTL (초)", example = "300")
    private Integer cacheTtlSeconds;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "생성 일시", example = "2025-01-01T00:00:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "수정 일시", example = "2025-01-01T00:00:00")
    private LocalDateTime updatedAt;

    /**
     * FeatureFlag 엔티티를 캐시 DTO로 변환
     * 
     * @param featureFlag 기능 플래그 엔티티
     * @return 캐시 DTO
     */
    public static FeatureFlagCacheDto from(FeatureFlag featureFlag) {
        if (featureFlag == null) {
            return null;
        }

        return FeatureFlagCacheDto.builder()
                .id(featureFlag.getId())
                .flagKey(featureFlag.getFlagKey())
                .flagName(featureFlag.getFlagName())
                .description(featureFlag.getDescription())
                .isEnabled(featureFlag.getIsEnabled())
                .status(featureFlag.getStatus())
                .targetTenants(featureFlag.getTargetTenants())
                .targetUsers(featureFlag.getTargetUsers())
                .rolloutPercentage(featureFlag.getRolloutPercentage())
                .startDate(featureFlag.getStartDate())
                .endDate(featureFlag.getEndDate())
                .metadata(featureFlag.getMetadata())
                .cacheTtlSeconds(featureFlag.getCacheTtlSeconds())
                .createdAt(featureFlag.getCreatedAt())
                .updatedAt(featureFlag.getUpdatedAt())
                .build();
    }

    /**
     * 캐시 TTL 계산 (cacheTtlSeconds가 null이면 기본값 300초 반환)
     * 
     * @param defaultTtl 기본 TTL (초)
     * @return 적용할 TTL (초)
     */
    public int getEffectiveTtl(int defaultTtl) {
        return cacheTtlSeconds != null ? cacheTtlSeconds : defaultTtl;
    }

    /**
     * TTL 유효성 검증 (10~3600초 범위)
     * 
     * @param minTtl 최소 TTL
     * @param maxTtl 최대 TTL
     * @return 유효 여부
     */
    public boolean isValidTtl(int minTtl, int maxTtl) {
        if (cacheTtlSeconds == null) {
            return true; // null은 기본값 사용이므로 유효
        }
        return cacheTtlSeconds >= minTtl && cacheTtlSeconds <= maxTtl;
    }
}

