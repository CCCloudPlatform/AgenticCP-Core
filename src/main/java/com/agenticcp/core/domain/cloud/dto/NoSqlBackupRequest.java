package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * NoSQL 백업/복원 요청 DTO
 *
 * CSP 중립적인 NoSQL 테이블 백업 및 복원 요청을 정의합니다.
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
public class NoSqlBackupRequest {

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
     * 대상 테이블 이름
     */
    @NotBlank(message = "테이블 이름은 필수입니다")
    private String tableName;

    /**
     * 리전
     */
    private String region;

    /**
     * 백업 이름 (생성 시)
     */
    private String backupName;

    /**
     * 백업 ID/ARN (삭제/복원 시)
     */
    private String backupId;

    /**
     * 복원 대상 새 테이블 이름 (복원 시)
     */
    private String targetTableName;

    /**
     * 시점 복원 시 기준 시각 (PITR 복원 시)
     */
    private Instant restorePointInTime;

    /**
     * 최신 시점 복원 여부 (PITR 복원 시)
     * true인 경우 restorePointInTime 무시
     */
    @Builder.Default
    private boolean useLatestRestorableTime = false;

    /**
     * 백업 검색 시작 시간 (목록 조회 시)
     */
    private Instant fromTime;

    /**
     * 백업 검색 종료 시간 (목록 조회 시)
     */
    private Instant toTime;

    /**
     * 백업 타입 필터 (목록 조회 시)
     */
    private BackupType backupTypeFilter;

    /**
     * 백업 타입
     */
    public enum BackupType {
        USER,           // 사용자 생성 온디맨드 백업
        SYSTEM,         // 시스템 자동 백업
        AWS_BACKUP,     // AWS Backup 서비스 백업
        ALL             // 모든 타입
    }
}

