package com.agenticcp.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 감사 로깅 설정
 * 
 * 감사 로깅에 필요한 Bean들을 설정합니다.
 * AOP를 통한 감사 로깅을 활성화합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
public class AuditConfig {
    
    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-async-");
        executor.initialize();
        return executor;
    }
}
