package com.agenticcp.core.common.logging;

import jakarta.servlet.http.HttpServletRequest;

public interface MdcContextProvider {
    void setContext(HttpServletRequest request);
}
