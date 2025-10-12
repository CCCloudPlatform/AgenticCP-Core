package com.agenticcp.core.domain.notification.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 알림 시스템 설정
 * 
 * <p>슬랙 웹훅 발송을 위한 RestTemplate 설정과 비동기 처리를 위한 설정을 제공합니다.</p>
 */
@Configuration
@EnableAsync
public class NotificationConfig {

    /**
     * 알림 발송용 RestTemplate 빈 설정
     * 
     * <p>슬랙, 디스코드 등 웹훅 기반 알림 발송에 사용됩니다.</p>
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))    // 연결 타임아웃 5초
                .setReadTimeout(Duration.ofSeconds(10))      // 읽기 타임아웃 10초
                .build();
    }

    /**
     * 알림 발송을 위한 비동기 실행자 설정
     * 
     * <p>알림 발송 작업을 비동기로 처리하기 위한 ThreadPoolTaskExecutor를 설정합니다.</p>
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // 기본 스레드 수
        executor.setMaxPoolSize(20);           // 최대 스레드 수
        executor.setQueueCapacity(100);        // 대기 큐 크기
        executor.setThreadNamePrefix("notification-");  // 스레드 이름 접두사
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 종료 시 대기
        executor.setAwaitTerminationSeconds(60);  // 종료 대기 시간
        executor.initialize();
        return executor;
    }
}

