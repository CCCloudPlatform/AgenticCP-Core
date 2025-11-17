package com.agenticcp.core.common.logging;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 요청으로부터 MDC 컨텍스트를 설정하는 전략 인터페이스입니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
public interface MdcContextProvider {
    void setContext(HttpServletRequest request);
}
