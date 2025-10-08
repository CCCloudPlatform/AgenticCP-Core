package com.agenticcp.core.domain.monitoring.storage;

import com.agenticcp.core.domain.monitoring.enums.StorageType;

import java.util.List;

/**
 * 메트릭 저장소 팩토리 인터페이스
 * 
 * <p>다양한 타입의 메트릭 저장소를 생성하고 관리하는 팩토리 인터페이스입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface MetricsStorageFactory {
    
    /**
     * 저장소 타입에 따른 MetricsStorage 생성
     * 
     * @param type 저장소 타입
     * @return 생성된 메트릭 저장소
     * @throws com.agenticcp.core.common.exception.BusinessException 저장소 생성 실패 시
     */
    MetricsStorage createStorage(StorageType type);
    
    /**
     * 활성화된 모든 저장소 생성
     * 
     * @return 활성화된 저장소 목록
     * @throws com.agenticcp.core.common.exception.BusinessException 저장소 생성 실패 시
     */
    List<MetricsStorage> createAllStorages();
    
    /**
     * 특정 타입의 저장소 존재 여부 확인
     * 
     * @param type 저장소 타입
     * @return 존재 여부
     */
    boolean hasStorage(StorageType type);
    
    /**
     * 저장소 활성화/비활성화 설정
     * 
     * @param type 저장소 타입
     * @param enabled 활성화 여부
     */
    void setStorageEnabled(StorageType type, boolean enabled);
    
    /**
     * 저장소 설정 정보 조회
     * 
     * @param type 저장소 타입
     * @return 저장소 설정 정보
     */
    StorageConfig getStorageConfig(StorageType type);
    
    /**
     * 저장소 설정 정보 업데이트
     * 
     * @param type 저장소 타입
     * @param config 새로운 설정 정보
     */
    void updateStorageConfig(StorageType type, StorageConfig config);
    
    /**
     * 저장소 설정 정보 클래스
     */
    class StorageConfig {
        private final boolean enabled;
        private final String url;
        private final String username;
        private final String password;
        private final String database;
        private final String pushgateway;
        private final int timeout;
        private final int retryCount;
        
        private StorageConfig(Builder builder) {
            this.enabled = builder.enabled;
            this.url = builder.url;
            this.username = builder.username;
            this.password = builder.password;
            this.database = builder.database;
            this.pushgateway = builder.pushgateway;
            this.timeout = builder.timeout;
            this.retryCount = builder.retryCount;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public Builder toBuilder() {
            return new Builder()
                    .enabled(enabled)
                    .url(url)
                    .username(username)
                    .password(password)
                    .database(database)
                    .pushgateway(pushgateway)
                    .timeout(timeout)
                    .retryCount(retryCount);
        }
        
        // Getters
        public boolean isEnabled() { return enabled; }
        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDatabase() { return database; }
        public String getPushgateway() { return pushgateway; }
        public int getTimeout() { return timeout; }
        public int getRetryCount() { return retryCount; }
        
        public static class Builder {
            private boolean enabled = true;
            private String url;
            private String username;
            private String password;
            private String database;
            private String pushgateway;
            private int timeout = 30000;
            private int retryCount = 3;
            
            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            
            public Builder url(String url) {
                this.url = url;
                return this;
            }
            
            public Builder username(String username) {
                this.username = username;
                return this;
            }
            
            public Builder password(String password) {
                this.password = password;
                return this;
            }
            
            public Builder database(String database) {
                this.database = database;
                return this;
            }
            
            public Builder pushgateway(String pushgateway) {
                this.pushgateway = pushgateway;
                return this;
            }
            
            public Builder timeout(int timeout) {
                this.timeout = timeout;
                return this;
            }
            
            public Builder retryCount(int retryCount) {
                this.retryCount = retryCount;
                return this;
            }
            
            public StorageConfig build() {
                return new StorageConfig(this);
            }
        }
    }
}
