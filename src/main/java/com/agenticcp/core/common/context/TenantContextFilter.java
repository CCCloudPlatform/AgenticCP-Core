package com.agenticcp.core.common.context;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 요청에서 테넌트 정보를 추출하여 TenantContextHolder에 설정하는 필터
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // 다른 필터보다 먼저 실행되도록 설정
public class TenantContextFilter extends OncePerRequestFilter {
    
    private final TenantService tenantService;
    
    private static final String TENANT_KEY_HEADER = "X-Tenant-Key";
    private static final String TENANT_KEY_PARAM = "tenantKey";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 테넌트 컨텍스트 설정
            setTenantContext(request);
            
            // 다음 필터 또는 컨트롤러로 요청 전달
            filterChain.doFilter(request, response);
            
        } finally {
            // 요청 처리 완료 후 테넌트 컨텍스트 정리
            TenantContextHolder.clear();
        }
    }
    
    /**
     * HTTP 요청에서 테넌트 정보를 추출하여 컨텍스트에 설정
     * 
     * @param request HTTP 요청 객체
     */
    private void setTenantContext(HttpServletRequest request) {
        String tenantKey = extractTenantKey(request);
        
        if (tenantKey != null) {
            try {
                // 테넌트 키로 테넌트 정보 조회
                Tenant tenant = tenantService.getTenantByKey(tenantKey)
                        .orElseThrow(() -> new IllegalStateException("Invalid tenant key: " + tenantKey));
                
                // 테넌트 컨텍스트에 설정
                TenantContextHolder.setTenant(tenant);
                
                log.debug("Tenant context set for request: {} -> {}", request.getRequestURI(), tenantKey);
                
            } catch (Exception e) {
                log.warn("Failed to set tenant context for key: {}, error: {}", tenantKey, e.getMessage());
                // 테넌트 컨텍스트 설정 실패 시에도 요청은 계속 진행
            }
        } else {
            log.debug("No tenant key found in request: {}", request.getRequestURI());
        }
    }
    
    /**
     * HTTP 요청에서 테넌트 키를 추출
     * 우선순위: Header > Query Parameter > Path Variable
     * 
     * @param request HTTP 요청 객체
     * @return 추출된 테넌트 키, 없으면 null
     */
    private String extractTenantKey(HttpServletRequest request) {
        // 1. Header에서 테넌트 키 추출
        String tenantKey = request.getHeader(TENANT_KEY_HEADER);
        if (tenantKey != null && !tenantKey.trim().isEmpty()) {
            return tenantKey.trim();
        }
        
        // 2. Query Parameter에서 테넌트 키 추출
        tenantKey = request.getParameter(TENANT_KEY_PARAM);
        if (tenantKey != null && !tenantKey.trim().isEmpty()) {
            return tenantKey.trim();
        }
        
        // 3. Path Variable에서 테넌트 키 추출 (예: /api/tenants/{tenantKey}/roles)
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/tenants/")) {
            String[] pathParts = requestURI.split("/");
            for (int i = 0; i < pathParts.length - 1; i++) {
                if ("tenants".equals(pathParts[i]) && i + 1 < pathParts.length) {
                    String extractedKey = pathParts[i + 1];
                    if (!extractedKey.isEmpty() && !extractedKey.equals("active") && 
                        !extractedKey.equals("type") && !extractedKey.equals("trial") && 
                        !extractedKey.equals("expired") && !extractedKey.equals("count")) {
                        return extractedKey;
                    }
                }
            }
        }
        
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 특정 경로는 테넌트 컨텍스트 필터를 적용하지 않음
        String requestURI = request.getRequestURI();
        return requestURI.startsWith("/api/health") || 
               requestURI.startsWith("/api/auth") ||
               requestURI.startsWith("/api/swagger") ||
               requestURI.startsWith("/api/v3/api-docs");
    }
}
