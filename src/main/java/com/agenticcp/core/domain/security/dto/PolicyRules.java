package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 정책 규칙 데이터 전송 객체
 * 
 * <p>정책의 규칙들을 정의하는 DTO입니다.
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
public class PolicyRules {
    
    /**
     * 기본 액션
     * 특정 규칙에 매칭되지 않을 때 적용되는 기본 액션
     */
    private String defaultAction;
    
    /**
     * 규칙 목록
     */
    private List<PolicyRule> rules;
    
    /**
     * 규칙 평가 모드
     * ALL: 모든 규칙이 만족되어야 함
     * ANY: 하나의 규칙만 만족되면 됨
     * FIRST: 첫 번째 만족되는 규칙만 적용
     */
    @Builder.Default
    private RuleEvaluationMode evaluationMode = RuleEvaluationMode.FIRST;
    
    /**
     * 규칙 평가 모드 열거형
     */
    public enum RuleEvaluationMode {
        ALL("모든 규칙 만족"),
        ANY("하나의 규칙 만족"),
        FIRST("첫 번째 규칙 적용");
        
        private final String description;
        
        RuleEvaluationMode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 규칙이 비어있는지 확인
     * 
     * @return 규칙이 비어있으면 true, 그렇지 않으면 false
     */
    public boolean isEmpty() {
        return rules == null || rules.isEmpty();
    }
    
    /**
     * 규칙 개수 반환
     * 
     * @return 규칙 개수
     */
    public int getRuleCount() {
        return rules == null ? 0 : rules.size();
    }
    
    /**
     * 특정 액션을 가진 규칙들 반환
     * 
     * @param action 액션
     * @return 해당 액션을 가진 규칙 목록
     */
    public List<PolicyRule> getRulesByAction(String action) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .filter(rule -> action.equals(rule.getAction()))
                .toList();
    }
}
