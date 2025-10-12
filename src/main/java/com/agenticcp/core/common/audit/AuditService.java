package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditContextProvider;
import com.agenticcp.core.common.dto.AuditContextDto;
import com.agenticcp.core.common.util.AuditInfoExtractor;
import com.agenticcp.core.common.util.ResponseDataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 감사 로깅 처리 서비스
 * 
 * Aspect에서 추출한 감사 메타데이터를 바탕으로 감사 로깅의 전체 흐름을 실행합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogger auditLogger;
    private final AuditContextProvider auditContextProvider;

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
            
        } catch (Exception e) {
            businessException = e;
            eventBuilder.success(false).error(e.getMessage());
        } finally {
            try {
                auditLogger.log(eventBuilder.build());
            } catch (Exception logException) {
                log.error("감사 로그 기록 중 오류 발생 [Action: {}]: {}", finalContext.action(), logException.getMessage(), logException);
            }
        }

        if (businessException != null) {
            throw businessException;
        }
        return result;
    }

    // TODO: [NEXT FEATURE] 마스킹 기능 구현
}
