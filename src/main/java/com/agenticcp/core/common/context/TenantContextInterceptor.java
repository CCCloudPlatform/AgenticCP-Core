package com.agenticcp.core.common.context;

import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.Worker;
import com.agenticcp.core.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Tenant Context Interceptor
 * 
 * <p>HTTP 헤더(`X-Tenant-Id`)에서 Tenant를 선택하고,
 * 해당 Tenant의 Worker를 자동 조회하여 TenantContextHolder에 저장합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {

    private final TenantContextService tenantContextService;
    private final UserRepository userRepository;

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();

        // 인증이 필요한 경로인지 확인 (인증이 필요 없는 경로는 스킵)
        if (shouldSkip(requestURI)) {
            return true;
        }

        // SecurityContext에서 User ID 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authentication found for request: {}", requestURI);
            return true; // 인증이 없으면 스킵 (다른 필터에서 처리)
        }

        Long userId = extractUserId(authentication);
        if (userId == null) {
            log.debug("No user ID found in authentication for request: {}", requestURI);
            return true;
        }

        // X-Tenant-Id 헤더에서 Tenant ID 추출
        String tenantIdHeader = request.getHeader(TENANT_ID_HEADER);
        if (tenantIdHeader == null || tenantIdHeader.trim().isEmpty()) {
            log.warn("Tenant ID header ({}) is required but not found in request: {}", TENANT_ID_HEADER, requestURI);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Tenant ID is required");
        }

        try {
            Long tenantId = Long.parseLong(tenantIdHeader.trim());

            // Tenant 선택 검증 및 Worker 조회
            Worker worker = tenantContextService.validateTenantAccessOrThrow(userId, tenantId);
            Tenant tenant = worker.getTenant();

            // TenantContextHolder에 Tenant와 Worker 저장
            TenantContextHolder.setCurrentTenantAndWorker(tenant, worker);

            log.debug("Tenant context set for request: {} -> tenantId={}, workerId={}", 
                requestURI, tenantId, worker.getId());

            return true;

        } catch (NumberFormatException e) {
            log.warn("Invalid tenant ID format in header: {}", tenantIdHeader);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Invalid tenant ID format");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // 요청 처리 완료 후 컨텍스트 정리
        TenantContextHolder.clear();
    }

    /**
     * 인증이 필요 없는 경로인지 확인
     * 
     * @param requestURI 요청 URI
     * @return 스킵 여부
     */
    private boolean shouldSkip(String requestURI) {
        return requestURI.startsWith("/health") ||
               requestURI.startsWith("/auth") ||
               requestURI.startsWith("/swagger") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/actuator");
    }

    /**
     * Authentication에서 User ID 추출
     * 
     * @param authentication Authentication 객체
     * @return User ID (없으면 null)
     */
    private Long extractUserId(Authentication authentication) {
        String username = authentication.getName();
        
        // username으로 User 조회
        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElse(null);
    }
}

