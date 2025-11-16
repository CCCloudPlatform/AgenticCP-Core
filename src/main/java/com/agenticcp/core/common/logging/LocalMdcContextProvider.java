package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.logging.masking.MaskingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬/개발 환경에서 MDC 컨텍스트를 설정하는 구현체입니다.
 * 기본적으로 클라이언트 IP만 설정합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Component
@Profile("!prod")
public class LocalMdcContextProvider extends AbstractMdcContextProvider {

    public LocalMdcContextProvider(MdcProperties mdcProperties, MaskingService maskingService) {
        super(mdcProperties, maskingService);
    }

    @Override
    public void setContext(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        putMdcSafely(MdcKeys.CLIENT_IP, clientIp);
    }
}