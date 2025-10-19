package com.agenticcp.core.domain.platform.cache.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 기능 플래그 변경 이벤트
 * <p>
 * Redis Pub/Sub을 통해 전파되는 플래그 변경 이벤트입니다.
 * 다른 노드에서 발생한 플래그 변경을 감지하여 로컬 캐시를 무효화합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 변경 이벤트 타입
     */
    public enum ChangeType {
        CREATED,    // 플래그 생성
        UPDATED,    // 플래그 수정
        DELETED,    // 플래그 삭제
        TOGGLED,    // 플래그 활성화/비활성화 토글
        INVALIDATED // 캐시 무효화 (수동)
    }

    /**
     * 플래그 키
     */
    private String flagKey;

    /**
     * 변경 타입
     */
    private ChangeType changeType;

    /**
     * 이벤트 발생 노드 ID (UUID)
     * <p>
     * 자신이 발행한 이벤트를 다시 처리하지 않기 위해 사용합니다.
     * </p>
     */
    private String sourceNodeId;

    /**
     * 이벤트 발생 시각
     */
    private LocalDateTime timestamp;

    /**
     * 이벤트를 발생시킨 사용자 (선택)
     */
    private String actorUserId;

    /**
     * 추가 메타데이터 (선택)
     */
    private String metadata;

    /**
     * 생성 이벤트 생성
     *
     * @param flagKey      플래그 키
     * @param sourceNodeId 발생 노드 ID
     * @return FeatureFlagChangeEvent
     */
    public static FeatureFlagChangeEvent created(String flagKey, String sourceNodeId) {
        return FeatureFlagChangeEvent.builder()
                .flagKey(flagKey)
                .changeType(ChangeType.CREATED)
                .sourceNodeId(sourceNodeId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 수정 이벤트 생성
     *
     * @param flagKey      플래그 키
     * @param sourceNodeId 발생 노드 ID
     * @return FeatureFlagChangeEvent
     */
    public static FeatureFlagChangeEvent updated(String flagKey, String sourceNodeId) {
        return FeatureFlagChangeEvent.builder()
                .flagKey(flagKey)
                .changeType(ChangeType.UPDATED)
                .sourceNodeId(sourceNodeId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 삭제 이벤트 생성
     *
     * @param flagKey      플래그 키
     * @param sourceNodeId 발생 노드 ID
     * @return FeatureFlagChangeEvent
     */
    public static FeatureFlagChangeEvent deleted(String flagKey, String sourceNodeId) {
        return FeatureFlagChangeEvent.builder()
                .flagKey(flagKey)
                .changeType(ChangeType.DELETED)
                .sourceNodeId(sourceNodeId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 토글 이벤트 생성
     *
     * @param flagKey      플래그 키
     * @param sourceNodeId 발생 노드 ID
     * @return FeatureFlagChangeEvent
     */
    public static FeatureFlagChangeEvent toggled(String flagKey, String sourceNodeId) {
        return FeatureFlagChangeEvent.builder()
                .flagKey(flagKey)
                .changeType(ChangeType.TOGGLED)
                .sourceNodeId(sourceNodeId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 무효화 이벤트 생성
     *
     * @param flagKey      플래그 키
     * @param sourceNodeId 발생 노드 ID
     * @return FeatureFlagChangeEvent
     */
    public static FeatureFlagChangeEvent invalidated(String flagKey, String sourceNodeId) {
        return FeatureFlagChangeEvent.builder()
                .flagKey(flagKey)
                .changeType(ChangeType.INVALIDATED)
                .sourceNodeId(sourceNodeId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

