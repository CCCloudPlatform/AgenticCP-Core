package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.logging.masking.MaskingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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