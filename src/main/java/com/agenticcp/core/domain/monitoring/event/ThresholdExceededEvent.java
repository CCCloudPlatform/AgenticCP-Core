package com.agenticcp.core.domain.monitoring.event;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 임계값 초과 이벤트
 * 
 * <p>메트릭이 임계값을 초과했을 때 발행됩니다.</p>
 * <p>Issue #15 가이드: 이벤트 기반 알림 시스템</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
public class ThresholdExceededEvent extends ApplicationEvent {
    
    /**
     * 임계값 규칙 (AlertRule 대신 MetricThreshold 사용)
     */
    private final MetricThreshold threshold;
    
    /**
     * 메트릭 값
     */
    private final Metric metric;
    
    /**
     * 이벤트 생성자
     * 
     * @param source 이벤트를 발행한 객체
     * @param threshold 임계값 규칙
     * @param metric 메트릭 값
     */
    public ThresholdExceededEvent(Object source, MetricThreshold threshold, Metric metric) {
        super(source);
        this.threshold = threshold;
        this.metric = metric;
    }
}

