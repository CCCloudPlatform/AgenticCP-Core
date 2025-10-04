package com.agenticcp.core.common.context;

import com.agenticcp.core.common.dto.AuditContextDto;
import com.agenticcp.core.common.logging.MdcKeys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 감사 컨텍스트 제공자
 * 
 * MDC에서 현재 요청의 컨텍스트 정보를 추출하여 제공하는 컴포넌트입니다.
 * 사용자 ID 추출 등의 로직을 캡슐화합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AuditContextProvider {

    /**
     * 현재 요청의 감사 컨텍스트 정보를 반환합니다.
     * 
     * @return AuditContext 현재 요청의 컨텍스트 정보
     */
    public AuditContextDto getCurrentContext() {
        return AuditContextDto.builder()
                .requestId(MDC.get(MdcKeys.REQUEST_ID))
                .tenantId(MDC.get(MdcKeys.TENANT_ID))
                .clientIp(MDC.get(MdcKeys.CLIENT_IP))
                .userId(getCurrentUserId())
                .action(null)
                .resourceType(null)
                .httpMethod(null)
                .requestPath(null)
                .operationSummary(null)
                .controllerName(null)
                .methodName(null)
                .severity(null)
                .includeRequestData(false)
                .includeResponseData(false)
                .build();
    }

    /**
     * 현재 인증된 사용자의 ID를 추출합니다.
     * 
     * @return 사용자 ID, 추출 실패 시 "unknown" 반환
     */
    private String getCurrentUserId() {
        // Spring Security 컨텍스트에서 사용자 ID 추출 로직
        try {
            // TODO: 실제 Spring Security 컨텍스트에서 사용자 ID 추출 로직 구현
            // SecurityContextHolder.getContext().getAuthentication() 등을 사용
            return "current_user"; // 임시 값
        } catch (Exception e) {
            log.warn("Failed to get current user ID: {}", e.getMessage());
            return "unknown";
        }
    }
}