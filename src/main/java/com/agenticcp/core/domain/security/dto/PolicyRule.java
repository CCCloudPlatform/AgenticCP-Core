package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 개별 정책 규칙 데이터 전송 객체
 * 
 * <p>정책의 개별 규칙을 정의하는 DTO입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {
    
    /**
     * 규칙 ID
     */
    private String ruleId;
    
    /**
     * 규칙 이름
     */
    private String ruleName;
    
    /**
     * 규칙 설명
     */
    private String description;
    
    /**
     * 규칙 조건
     * 예: "user.role == 'ADMIN'", "resource.type == 'EC2'"
     */
    private String condition;
    
    /**
     * 규칙이 만족될 때 수행할 액션
     * ALLOW, DENY, INCONCLUSIVE
     */
    private String action;
    
    /**
     * 규칙 우선순위
     * 높을수록 우선 적용
     */
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * 규칙 활성화 여부
     */
    @Builder.Default
    private Boolean enabled = true;
    
    /**
     * 규칙 매개변수
     */
    private Map<String, Object> parameters;
    
    /**
     * 규칙 태그
     */
    private List<String> tags;
    
    /**
     * 규칙 생성 시간
     */
    private String createdAt;
    
    /**
     * 규칙 수정 시간
     */
    private String updatedAt;
    
    /**
     * 규칙 버전
     */
    @Builder.Default
    private String version = "1.0";
    
    /**
     * 규칙이 활성화되어 있는지 확인
     * 
     * @return 활성화되어 있으면 true, 그렇지 않으면 false
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * 매개변수에서 값을 가져옴
     * 
     * @param key 매개변수 키
     * @return 매개변수 값
     */
    public Object getParameter(String key) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(key);
    }
    
    /**
     * 매개변수에 값을 설정
     * 
     * @param key 매개변수 키
     * @param value 매개변수 값
     */
    public void setParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new java.util.HashMap<>();
        }
        parameters.put(key, value);
    }
    
    /**
     * 특정 태그가 있는지 확인
     * 
     * @param tag 확인할 태그
     * @return 태그가 있으면 true, 그렇지 않으면 false
     */
    public boolean hasTag(String tag) {
        if (tags == null) {
            return false;
        }
        return tags.contains(tag);
    }
    
    /**
     * 태그 추가
     * 
     * @param tag 추가할 태그
     */
    public void addTag(String tag) {
        if (tags == null) {
            tags = new java.util.ArrayList<>();
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
    
    /**
     * 규칙이 허용 액션인지 확인
     * 
     * @return 허용 액션이면 true, 그렇지 않으면 false
     */
    public boolean isAllowAction() {
        return "ALLOW".equalsIgnoreCase(action);
    }
    
    /**
     * 규칙이 거부 액션인지 확인
     * 
     * @return 거부 액션이면 true, 그렇지 않으면 false
     */
    public boolean isDenyAction() {
        return "DENY".equalsIgnoreCase(action);
    }
    
    @Override
    public String toString() {
        return String.format("PolicyRule{ruleId='%s', ruleName='%s', condition='%s', action='%s', priority=%d, enabled=%s}", 
            ruleId, ruleName, condition, action, priority, enabled);
    }
}
