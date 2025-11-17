package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.logging.masking.MaskingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬/개발 환경에서 MDC 컨텍스트를 설정하는 구현체입니다.
 * 기본적으로 클라이언트 IP만 설정하며, 마스킹 설정은 상위 클래스에 위임합니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Component
@Profile("!prod")
public class LocalMdcContextProvider extends AbstractMdcContextProvider {

    public LocalMdcContextProvider(MdcProperties mdcProperties, MaskingService maskingService) {
        super(mdcProperties, maskingService);
    }

    /**
     * 클라이언트 IP를 추출하여 {@link MdcKeys#CLIENT_IP}에 설정합니다.
     *
     * @param request 현재 HTTP 요청
     */
    @Override
    public void setContext(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        putMdcSafely(MdcKeys.CLIENT_IP, clientIp);
    }
}