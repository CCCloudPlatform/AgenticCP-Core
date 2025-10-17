package com.agenticcp.core.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

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
public class AuditConfig {
}
