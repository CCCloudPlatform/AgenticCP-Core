package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 감사 로그 파일 로깅 리스너
 * 
 * AuditPublishEvent를 받아서 파일에 감사 로그를 기록합니다.
 * 동기로 동작하여 즉시 파일에 로그를 남깁니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditFileLogListener {

    private final AuditLogger auditLogger;

    @EventListener
    public void handleAuditEvent(AuditPublishEvent event) {
        try {
            AuditEventDto auditEventDto = event.getAuditEventDto();
            
            auditLogger.log(auditEventDto);
            log.debug("감사 로그 파일 기록 완료 [Action: {}, RequestId: {}]", 
                     auditEventDto.action(), auditEventDto.requestId());

        } catch (Exception e) {
            log.error("감사 로그 파일 기록 실패 [Action: {}]: {}",
                     event.getAuditEventDto().action(), e.getMessage(), e);
        }
    }
}

