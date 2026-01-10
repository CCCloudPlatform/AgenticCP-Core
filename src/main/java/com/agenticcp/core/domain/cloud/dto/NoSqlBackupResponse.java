package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * NoSQL 백업 응답 DTO
 *
 * NoSQL 테이블 백업 정보를 반환합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class NoSqlBackupResponse {

    /**
     * 백업 ARN 또는 고유 식별자
     */
    private String backupId;

    /**
     * 백업 이름
     */
    private String backupName;

    /**
     * 클라우드 프로바이더 타입
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프
     */
    private String accountScope;

    /**
     * 대상 테이블 ARN
     */
    private String tableArn;

    /**
     * 대상 테이블 이름
     */
    private String tableName;

    /**
     * 리전
     */
    private String region;

    /**
     * 백업 상태
     */
    private String status;

    /**
     * 백업 타입
     */
    private String backupType;

    /**
     * 백업 크기 (바이트)
     */
    private Long backupSizeBytes;

    /**
     * 백업 생성 시간
     */
    private Instant backupCreationTime;

    /**
     * 백업 만료 시간 (설정된 경우)
     */
    private Instant backupExpiryTime;

    /**
     * CloudResource에서 NoSqlBackupResponse로 변환
     */
    public static NoSqlBackupResponse from(CloudResource resource) {
        return NoSqlBackupResponse.builder()
                .backupId(resource.getResourceId())
                .backupName(resource.getResourceName())
                .providerType(resource.getProvider() != null ? resource.getProvider().getProviderType() : null)
                .region(resource.getRegion() != null ? resource.getRegion().getRegionKey() : null)
                .status(resource.getLifecycleState() != null ? resource.getLifecycleState().name() : null)
                .build();
    }
}

