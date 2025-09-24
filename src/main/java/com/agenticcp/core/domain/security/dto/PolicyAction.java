package com.agenticcp.core.domain.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 정책 액션 데이터 전송 객체
 * 
 * <p>정책 위반 시 수행할 액션을 정의하는 DTO입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAction {
    
    /**
     * 액션 타입
     */
    private ActionType type;
    
    /**
     * 액션 설명
     */
    private String description;
    
    /**
     * 액션 지속 시간
     */
    private Duration duration;
    
    /**
     * 액션 매개변수
     */
    private Map<String, Object> parameters;
    
    /**
     * 액션 우선순위
     */
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * 액션 실행 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime executedAt;
    
    /**
     * 액션 실행 상태
     */
    private ActionStatus status;
    
    /**
     * 액션 실행 결과 메시지
     */
    private String resultMessage;
    
    /**
     * 액션 타입 열거형
     */
    public enum ActionType {
        BLOCK_USER("사용자 차단"),
        BLOCK_IP("IP 차단"),
        REQUIRE_2FA("2단계 인증 요구"),
        SEND_ALERT("알림 발송"),
        QUARANTINE_RESOURCE("리소스 격리"),
        ESCALATE_TO_ADMIN("관리자에게 에스컬레이션"),
        LOG_VIOLATION("위반 로깅"),
        NOTIFY_USER("사용자 알림"),
        SUSPEND_ACCOUNT("계정 일시정지"),
        REVOKE_ACCESS("접근 권한 취소");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 액션 실행 상태 열거형
     */
    public enum ActionStatus {
        PENDING("대기 중"),
        EXECUTING("실행 중"),
        COMPLETED("완료"),
        FAILED("실패"),
        CANCELLED("취소됨");
        
        private final String description;
        
        ActionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 액션 매개변수에서 값을 가져옴
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
     * 액션 매개변수에 값을 설정
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
     * 액션이 완료되었는지 확인
     * 
     * @return 완료되었으면 true, 그렇지 않으면 false
     */
    public boolean isCompleted() {
        return status == ActionStatus.COMPLETED;
    }
    
    /**
     * 액션이 실패했는지 확인
     * 
     * @return 실패했으면 true, 그렇지 않으면 false
     */
    public boolean isFailed() {
        return status == ActionStatus.FAILED;
    }
    
    /**
     * 액션이 실행 중인지 확인
     * 
     * @return 실행 중이면 true, 그렇지 않으면 false
     */
    public boolean isExecuting() {
        return status == ActionStatus.EXECUTING;
    }
    
    @Override
    public String toString() {
        return String.format("PolicyAction{type=%s, description='%s', status=%s, executedAt=%s}", 
            type, description, status, executedAt);
    }
}
