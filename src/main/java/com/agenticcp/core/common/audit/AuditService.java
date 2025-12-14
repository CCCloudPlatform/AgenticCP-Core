package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditChangeContext;
import com.agenticcp.core.common.context.AuditContextProvider;
import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.util.AuditInfoExtractor;
import com.agenticcp.core.common.util.ChangeTracker;
import com.agenticcp.core.common.util.ResponseDataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 감사 로깅 처리 서비스입니다.
 * 추출된 감사 컨텍스트로 이벤트를 생성하여 발행합니다.
 * 파일/DB 기록은 리스너에서 수행됩니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final ApplicationEventPublisher eventPublisher;
    private final AuditContextProvider auditContextProvider;
    private final ChangeTracker changeTracker;

    /**
     * 감사 컨텍스트를 기반으로 대상 메서드를 실행하고 감사 이벤트를 발행합니다.
     *
     * @param joinPoint 감사 대상 조인 포인트
     * @param auditInfo 감사 컨텍스트 정보
     * @return 원본 메서드 실행 결과
     * @throws Throwable 원본 메서드 또는 감사 처리 중 발생한 예외
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                log.error("감사 요청 데이터 추출 실패 [Action: {}, RequestId: {}, TenantId: {}]",
                        finalContext.action(), finalContext.requestId(), finalContext.tenantId(), auditException);
                throw new BusinessException(AuditErrorCode.AUDIT_DATA_EXTRACTION_FAILED);
            }
            result = joinPoint.proceed();

            try {
                if (finalContext.includeResponseData()) {
                    Map<String, Object> extractedData = ResponseDataExtractor.extract(result);
                    eventBuilder.responseData(extractedData);
                }
            } catch (Exception auditException) {
                log.error("감사 응답 데이터 추출 실패 [Action: {}, RequestId: {}, TenantId: {}]",
                        finalContext.action(), finalContext.requestId(), finalContext.tenantId(), auditException);
                throw new BusinessException(AuditErrorCode.AUDIT_DATA_EXTRACTION_FAILED);
            }
            eventBuilder.success(true);
            
            try {
                Map<String, Object> newValue = changeTracker.extractNewValue(result);
                if (newValue != null && !newValue.isEmpty()) {
                    eventBuilder.newValue(newValue);
                }
            } catch (Exception changeException) {
                log.error("감사 변경 데이터 추출 실패 [Action: {}, RequestId: {}, TenantId: {}]",
                        finalContext.action(), finalContext.requestId(), finalContext.tenantId(), changeException);
                throw new BusinessException(AuditErrorCode.AUDIT_DATA_EXTRACTION_FAILED);
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
                
                log.debug("감사 이벤트 발행 완료 [Action: {}, Success: {}, RequestId: {}, TenantId: {}]", 
                         auditEventDto.action(), auditEventDto.success(),
                         auditEventDto.requestId(), auditEventDto.tenantId());
                
            } catch (Exception publishException) {
                log.error("감사 이벤트 발행 중 오류 발생 [Action: {}, RequestId: {}, TenantId: {}]",
                        finalContext.action(), finalContext.requestId(), finalContext.tenantId(), publishException);
                if (businessException == null) {
                    businessException = new BusinessException(AuditErrorCode.AUDIT_EVENT_PUBLISH_FAILED);
                }
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
