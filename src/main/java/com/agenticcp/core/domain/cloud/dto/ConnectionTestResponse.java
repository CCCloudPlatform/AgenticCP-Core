package com.agenticcp.core.domain.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 클라우드 계정 연결 테스트 결과 DTO
 * 등록된 계정의 연결 상태를 테스트한 결과를 담습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResponse {
    
    /**
     * 연결 테스트 성공 여부
     */
    private Boolean success;
    
    /**
     * 테스트 결과 메시지
     */
    private String message;
    
    /**
     * 계정 ID
     */
    private Long accountId;
    
    /**
     * 테스트 수행 시간
     */
    private LocalDateTime testedAt;
    
    /**
     * 추가 상세 정보
     * 응답 시간, 연결된 리전, 서비스 상태 등
     */
    private Map<String, Object> details;
}

