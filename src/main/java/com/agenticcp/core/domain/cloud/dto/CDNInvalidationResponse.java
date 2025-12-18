package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.port.model.cdn.InvalidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDN 캐시 무효화 응답 DTO
 * 
 * <p>외부 API 클라이언트에게 반환되는 캐시 무효화 결과입니다.</p>
 * <p>포트 모델(InvalidationResult)을 DTO로 변환하여 인터페이스 계층과 도메인 계층을 분리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CDNInvalidationResponse {

    /**
     * 무효화 ID
     */
    private String invalidationId;

    /**
     * Distribution ID
     */
    private String distributionId;

    /**
     * 무효화 상태
     * - InProgress: 진행 중
     * - Completed: 완료
     */
    private String status;

    /**
     * 생성 시간
     */
    private LocalDateTime createTime;

    /**
     * 무효화된 경로 목록
     */
    private List<String> paths;

    /**
     * InvalidationResult에서 CDNInvalidationResponse로 변환
     * 
     * @param result 포트 모델 (도메인 계층)
     * @return API 응답 DTO (인터페이스 계층)
     */
    public static CDNInvalidationResponse from(InvalidationResult result) {
        return CDNInvalidationResponse.builder()
                .invalidationId(result.invalidationId())
                .distributionId(result.distributionId())
                .status(result.status())
                .createTime(result.createTime())
                .paths(result.paths())
                .build();
    }
}

