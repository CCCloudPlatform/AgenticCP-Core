package com.agenticcp.core.domain.platform.cache.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 분산 락 설정 클래스
 * 기능 플래그 업데이트 시 동시성 제어를 위한 분산 락 제공
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Configuration
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${redisson.threads:16}")
    private int threads;

    @Value("${redisson.lock-watchdog-timeout:30000}")
    private long lockWatchdogTimeout;

    /**
     * RedissonClient 빈 생성
     * 분산 락 및 분산 자료구조 지원
     * 
     * @return RedissonClient
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 단일 서버 모드 설정
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setTimeout(3000)
                .setConnectTimeout(10000);

        // 스레드 풀 설정
        config.setThreads(threads);
        config.setNettyThreads(threads);

        // Watch Dog 타임아웃 설정 (락 자동 해제)
        config.setLockWatchdogTimeout(lockWatchdogTimeout);

        return Redisson.create(config);
    }
}

