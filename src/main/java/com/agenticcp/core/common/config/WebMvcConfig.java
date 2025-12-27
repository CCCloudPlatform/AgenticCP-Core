package com.agenticcp.core.common.config;

import com.agenticcp.core.common.context.TenantContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * Interceptor 등록
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/health",
                "/auth/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/actuator/**"
            );
    }
}

