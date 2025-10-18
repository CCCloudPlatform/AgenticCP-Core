package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditChangeContext;
import com.agenticcp.core.common.context.AuditContextProvider;
import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.util.AuditInfoExtractor;
import com.agenticcp.core.common.util.ChangeTracker;
import com.agenticcp.core.common.util.ResponseDataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 감사 로깅 처리 서비스
 * 
 * Aspect에서 추출한 감사 메타데이터를 바탕으로 감사 이벤트를 생성하고 발행합니다.
 * 실제 로깅 및 DB 저장은 이벤트 리스너들이 처리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final ApplicationEventPublisher eventPublisher;
    private final AuditContextProvider auditContextProvider;
    private final ChangeTracker changeTracker;

    public Object audit(ProceedingJoinPoint joinPoint, AuditContextDto auditInfo) throws Throwable {
        var mdcContext = auditContextProvider.getCurrentContext();
        AuditContextDto finalContext = auditInfo.toBuilder()
                .requestId(mdcContext.requestId())
                .tenantId(mdcContext.tenantId())
                .clientIp(mdcContext.clientIp())
                .userId(mdcContext.userId())
                .build();

        AuditEventBuilder eventBuilder = AuditEventBuilder.builder(finalContext);

        Object result = null;
        Exception businessException = null;
        
        try {
            try {
                if (finalContext.includeRequestData()) {
                    Map<String, Object> requestData = AuditInfoExtractor.extractRequestData(joinPoint);
                    eventBuilder.requestData(requestData);
                }
            } catch (Exception auditException) {
                log.warn("감사 요청 데이터 추출 중 오류 발생 [Action: {}]: {}", finalContext.action(), auditException.getMessage());
            }
            result = joinPoint.proceed();

            try {
                if (finalContext.includeResponseData()) {
                    Map<String, Object> extractedData = ResponseDataExtractor.extract(result);
                    eventBuilder.responseData(extractedData);
                }
            } catch (Exception auditException) {
                log.warn("감사 응답 데이터 추출 중 오류 발생 [Action: {}]: {}", finalContext.action(), auditException.getMessage());
            }
            eventBuilder.success(true);
            
            try {
                Map<String, Object> newValue = changeTracker.extractNewValue(result);
                if (newValue != null && !newValue.isEmpty()) {
                    eventBuilder.newValue(newValue);
                }
            } catch (Exception changeException) {
                log.debug("변경 후 값 추출 실패 (무시): {}", changeException.getMessage());
            }
            
        } catch (Exception e) {
            businessException = e;
            eventBuilder.success(false).error(e.getMessage());
        } finally {
            try {
                Map<String, Object> oldValue = AuditChangeContext.getOldValue();
                String targetResourceId = AuditChangeContext.getTargetResourceId();
                
                if (oldValue != null) {
                    eventBuilder.oldValue(oldValue);
                    log.debug("변경 전 값 포함 [Action: {}]", finalContext.action());
                }
                
                if (targetResourceId != null) {
                    eventBuilder.targetResourceId(targetResourceId);
                }
                
                AuditEventDto auditEventDto = eventBuilder.build();
                AuditPublishEvent event = new AuditPublishEvent(this, auditEventDto);
                eventPublisher.publishEvent(event);
                
                log.debug("감사 이벤트 발행 완료 [Action: {}, Success: {}]", 
                         auditEventDto.action(), auditEventDto.success());
                
            } catch (Exception publishException) {
                log.error("감사 이벤트 발행 중 오류 발생 [Action: {}]: {}", 
                         finalContext.action(), publishException.getMessage(), publishException);
            } finally {
                AuditChangeContext.clear();
            }
        }

        if (businessException != null) {
            throw businessException;
        }
        return result;
    }

}
