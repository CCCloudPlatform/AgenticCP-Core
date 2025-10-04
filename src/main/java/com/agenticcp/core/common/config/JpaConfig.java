package com.agenticcp.core.common.config;

import com.agenticcp.core.common.interceptor.TenantAwareInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정 클래스
 * Hibernate Interceptor를 통한 테넌트 데이터 격리 설정
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Autowired
    private TenantAwareInterceptor tenantAwareInterceptor;

    /**
     * Hibernate 속성 커스터마이저
     * StatementInspector를 통해 SQL 쿼리 인터셉션 설정
     * 
     * @return HibernatePropertiesCustomizer
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // StatementInspector 설정 (Hibernate 5.4+)
            hibernateProperties.put(AvailableSettings.STATEMENT_INSPECTOR, tenantAwareInterceptor);
            
            // Interceptor 설정 (추가적인 엔티티 레벨 처리용)
            hibernateProperties.put(AvailableSettings.INTERCEPTOR, tenantAwareInterceptor);
            
            // SQL 로깅 활성화 (개발 환경에서 테넌트 필터링 확인용)
            hibernateProperties.put("hibernate.show_sql", false);
            hibernateProperties.put("hibernate.format_sql", true);
        };
    }
}
