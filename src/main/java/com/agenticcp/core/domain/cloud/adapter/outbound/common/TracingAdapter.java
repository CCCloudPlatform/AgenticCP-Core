package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 추적 포트 어댑터
 * 
 * TracingPort 인터페이스를 구현하여 분산 추적 기능을 제공합니다.
 * MDC(Mapped Diagnostic Context)를 활용한 컨텍스트 전파와 로깅 기반 추적을 제공합니다.
 * 향후 실제 추적 시스템(예: Jaeger, Zipkin, OpenTelemetry)과 연동할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class TracingAdapter implements TracingPort {

    @Value("${tracing.enabled:true}")
    private boolean tracingEnabled;
    
    @Value("${tracing.correlation-id-header:X-Correlation-ID}")
    private String correlationIdHeader;
    
    // 스팬 컨텍스트 저장소 (ThreadLocal 기반)
    private final ThreadLocal<SpanContext> spanContext = new ThreadLocal<>();
    
    // 활성 스팬 추적 (디버깅용)
    private final Map<String, SpanInfo> activeSpans = new ConcurrentHashMap<>();

    @Override
    public AutoCloseable startSpan(String name, Map<String, String> tags) {
        if (!tracingEnabled) {
            return createNoOpSpan();
        }
        
        try {
            log.debug("[Tracing] Starting span: name={}, tags={}", name, tags);
            
            // 현재 스팬 컨텍스트 가져오기
            SpanContext currentContext = spanContext.get();
            
            // 새 스팬 컨텍스트 생성
            String spanId = generateSpanId();
            String traceId = currentContext != null ? currentContext.getTraceId() : generateTraceId();
            String parentSpanId = currentContext != null ? currentContext.getSpanId() : null;
            
            SpanContext newContext = new SpanContext(traceId, spanId, parentSpanId, name, tags);
            spanContext.set(newContext);
            
            // MDC에 추적 정보 설정
            updateMdcWithSpanInfo(newContext);
            
            // 활성 스팬 등록
            SpanInfo spanInfo = new SpanInfo(name, tags, System.currentTimeMillis());
            activeSpans.put(spanId, spanInfo);
            
            log.debug("[Tracing] Span started: traceId={}, spanId={}, parentSpanId={}, name={}", 
                    traceId, spanId, parentSpanId, name);
            
            return createSpanCloser(spanId, name);
            
        } catch (Exception e) {
            log.error("[Tracing] Failed to start span: name={}, error={}", name, e.getMessage(), e);
            return createNoOpSpan();
        }
    }

    @Override
    public void tag(String key, String value) {
        if (!tracingEnabled) {
            return;
        }
        
        try {
            SpanContext currentContext = spanContext.get();
            if (currentContext != null) {
                currentContext.addTag(key, value);
                
                // MDC 업데이트
                MDC.put("span.tag." + key, value);
                
                log.debug("[Tracing] Tag added: {}={}", key, value);
            } else {
                log.warn("[Tracing] No active span context for adding tag: {}={}", key, value);
            }
        } catch (Exception e) {
            log.error("[Tracing] Failed to add tag: {}={}, error={}", key, value, e.getMessage(), e);
        }
    }

    @Override
    public void event(String name, Map<String, String> attrs) {
        if (!tracingEnabled) {
            return;
        }
        
        try {
            SpanContext currentContext = spanContext.get();
            if (currentContext != null) {
                log.debug("[Tracing] Event: name={}, attrs={}, traceId={}, spanId={}", 
                        name, attrs, currentContext.getTraceId(), currentContext.getSpanId());
                
                // 실제 추적 시스템에서는 여기서 이벤트를 기록
                // 예: jaegerTracer.activeSpan().logEvent(name, attrs);
                
            } else {
                log.warn("[Tracing] No active span context for adding event: {}", name);
            }
        } catch (Exception e) {
            log.error("[Tracing] Failed to add event: name={}, error={}", name, e.getMessage(), e);
        }
    }
    
    /**
     * No-op 스팬 생성 (추적 비활성화 시)
     */
    private AutoCloseable createNoOpSpan() {
        return () -> {
            // 아무것도 하지 않음
        };
    }
    
    /**
     * 스팬 종료 처리
     */
    private AutoCloseable createSpanCloser(String spanId, String spanName) {
        return () -> {
            try {
                SpanContext currentContext = spanContext.get();
                if (currentContext != null && spanId.equals(currentContext.getSpanId())) {
                    // 부모 스팬으로 복원
                    SpanContext parentContext = currentContext.getParentContext();
                    spanContext.set(parentContext);
                    
                    if (parentContext != null) {
                        updateMdcWithSpanInfo(parentContext);
                    } else {
                        clearMdcSpanInfo();
                    }
                    
                    // 활성 스팬에서 제거
                    SpanInfo removedSpan = activeSpans.remove(spanId);
                    if (removedSpan != null) {
                        long duration = System.currentTimeMillis() - removedSpan.getStartTime();
                        log.debug("[Tracing] Span closed: spanId={}, name={}, duration={}ms", 
                                spanId, spanName, duration);
                    }
                }
            } catch (Exception e) {
                log.error("[Tracing] Failed to close span: spanId={}, name={}, error={}", 
                        spanId, spanName, e.getMessage(), e);
            }
        };
    }
    
    /**
     * MDC에 스팬 정보 업데이트
     */
    private void updateMdcWithSpanInfo(SpanContext context) {
        MDC.put("traceId", context.getTraceId());
        MDC.put("spanId", context.getSpanId());
        MDC.put("spanName", context.getSpanName());
        
        if (context.getParentSpanId() != null) {
            MDC.put("parentSpanId", context.getParentSpanId());
        }
        
        // 태그들을 MDC에 추가
        context.getTags().forEach((key, value) -> 
            MDC.put("span.tag." + key, value));
    }
    
    /**
     * MDC에서 스팬 정보 제거
     */
    private void clearMdcSpanInfo() {
        MDC.remove("traceId");
        MDC.remove("spanId");
        MDC.remove("spanName");
        MDC.remove("parentSpanId");
        
        // span.tag.* 키들 제거
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        if (mdcMap != null) {
            mdcMap.keySet().stream()
                .filter(key -> key.startsWith("span.tag."))
                .forEach(MDC::remove);
        }
    }
    
    /**
     * 스팬 ID 생성
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 트레이스 ID 생성
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
    
    /**
     * 현재 활성 스팬 수 반환
     */
    public int getActiveSpanCount() {
        return activeSpans.size();
    }
    
    /**
     * 모든 활성 스팬 정보 반환
     */
    public Map<String, SpanInfo> getActiveSpans() {
        return Map.copyOf(activeSpans);
    }
    
    // 내부 클래스들
    private static class SpanContext {
        private final String traceId;
        private final String spanId;
        private final String parentSpanId;
        private final String spanName;
        private final Map<String, String> tags;
        private final SpanContext parentContext;
        
        public SpanContext(String traceId, String spanId, String parentSpanId, String spanName, Map<String, String> tags) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.spanName = spanName;
            this.tags = new ConcurrentHashMap<>(tags != null ? tags : Map.of());
            this.parentContext = null; // 부모 컨텍스트는 별도로 관리
        }
        
        public void addTag(String key, String value) {
            tags.put(key, value);
        }
        
        // Getters
        public String getTraceId() { return traceId; }
        public String getSpanId() { return spanId; }
        public String getParentSpanId() { return parentSpanId; }
        public String getSpanName() { return spanName; }
        public Map<String, String> getTags() { return Map.copyOf(tags); }
        public SpanContext getParentContext() { return parentContext; }
    }
    
    private static class SpanInfo {
        private final String name;
        private final Map<String, String> tags;
        private final long startTime;
        
        public SpanInfo(String name, Map<String, String> tags, long startTime) {
            this.name = name;
            this.tags = Map.copyOf(tags != null ? tags : Map.of());
            this.startTime = startTime;
        }
        
        public String getName() { return name; }
        public Map<String, String> getTags() { return tags; }
        public long getStartTime() { return startTime; }
    }
}
