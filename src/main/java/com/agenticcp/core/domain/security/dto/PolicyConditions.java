package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// import java.util.List;

/**
 * 정책 조건 데이터 전송 객체
 * 
 * <p>정책의 조건들을 정의하는 DTO입니다.
 * JSON 형태로 저장되어 정책 엔진에서 파싱하여 사용됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyConditions {
    
    /**
     * 시간 조건
     */
    private TimeConditions timeConditions;
    
    /**
     * IP 조건
     */
    private IpConditions ipConditions;
    
    /**
     * 사용자 조건
     */
    private UserConditions userConditions;
    
    /**
     * 리소스 조건
     */
    private ResourceConditions resourceConditions;
    
    /**
     * 네트워크 조건
     */
    private NetworkConditions networkConditions;
    
    /**
     * 환경 조건
     */
    private EnvironmentConditions environmentConditions;
    
    /**
     * 조건 평가 모드
     * ALL: 모든 조건이 만족되어야 함
     * ANY: 하나의 조건만 만족되면 됨
     */
    @Builder.Default
    private ConditionEvaluationMode evaluationMode = ConditionEvaluationMode.ALL;
    
    /**
     * 조건 평가 모드 열거형
     */
    public enum ConditionEvaluationMode {
        ALL("모든 조건 만족"),
        ANY("하나의 조건 만족");
        
        private final String description;
        
        ConditionEvaluationMode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 조건이 비어있는지 확인
     * 
     * @return 조건이 비어있으면 true, 그렇지 않으면 false
     */
    public boolean isEmpty() {
        return timeConditions == null && 
               ipConditions == null && 
               userConditions == null && 
               resourceConditions == null && 
               networkConditions == null && 
               environmentConditions == null;
    }
    
    /**
     * 활성화된 조건 개수 반환
     * 
     * @return 활성화된 조건 개수
     */
    public int getActiveConditionCount() {
        int count = 0;
        if (timeConditions != null) count++;
        if (ipConditions != null) count++;
        if (userConditions != null) count++;
        if (resourceConditions != null) count++;
        if (networkConditions != null) count++;
        if (environmentConditions != null) count++;
        return count;
    }
}
