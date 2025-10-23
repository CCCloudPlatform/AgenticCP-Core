package com.agenticcp.core.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정 클래스
 * 
 * 테넌트 설정 상속 시스템을 위한 캐시 설정을 제공합니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 매니저 빈 설정
     * 개발/테스트 환경에서는 ConcurrentMapCacheManager 사용
     * 운영 환경에서는 Redis CacheManager로 변경 권장
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 캐시 이름 설정
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "tenantConfigs",      // 테넌트별 전체 설정
            "tenantConfig",       // 테넌트별 개별 설정
            "platformConfigs",    // 플랫폼 설정
            "tenantTypeConfigs"   // 테넌트 타입별 설정
        ));
        
        // 캐시 생성 허용 (동적으로 캐시 생성 가능)
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}



