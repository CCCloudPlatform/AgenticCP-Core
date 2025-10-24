package com.agenticcp.core.domain.cloud.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 연결 테스트 결과 DTO
 * 
 * 클라우드 계정 연결 테스트 결과를 클라이언트에 반환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "연결 테스트 결과")
public class ConnectionTestResult {
    
    @Schema(description = "연결 성공 여부", example = "true")
    private boolean connected;
    
    @Schema(description = "계정 ID", example = "1")
    private Long accountId;
    
    @Schema(description = "클라우드 계정 ID", example = "123456789012")
    private String cloudAccountId;
    
    @Schema(description = "프로바이더 타입", example = "AWS")
    private String providerType;
    
    @Schema(description = "응답 시간 (ms)", example = "234")
    private Long responseTimeMs;
    
    @Schema(description = "결과 메시지", example = "AWS 계정 연결 성공: IAM Role 검증 완료")
    private String message;
    
    @Schema(description = "에러 코드 (실패 시)", example = "AWS_AUTH_FAILED")
    private String errorCode;
    
    @Schema(description = "에러 상세 (실패 시)", example = "IAM Role ARN이 유효하지 않습니다")
    private String errorDetail;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "테스트 수행 시간", example = "2025-10-24 10:30:00")
    private LocalDateTime testedAt;
    
    /**
     * 연결 성공 결과 생성 팩토리 메서드
     */
    public static ConnectionTestResult success(Long accountId, String cloudAccountId, String providerType, Long responseTimeMs) {
        return ConnectionTestResult.builder()
                .connected(true)
                .accountId(accountId)
                .cloudAccountId(cloudAccountId)
                .providerType(providerType)
                .responseTimeMs(responseTimeMs)
                .message(String.format("%s 계정 연결 성공", providerType))
                .testedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 연결 실패 결과 생성 팩토리 메서드
     */
    public static ConnectionTestResult failure(Long accountId, String cloudAccountId, String providerType, 
                                               String errorCode, String errorDetail) {
        return ConnectionTestResult.builder()
                .connected(false)
                .accountId(accountId)
                .cloudAccountId(cloudAccountId)
                .providerType(providerType)
                .message(String.format("%s 계정 연결 실패", providerType))
                .errorCode(errorCode)
                .errorDetail(errorDetail)
                .testedAt(LocalDateTime.now())
                .build();
    }
}

