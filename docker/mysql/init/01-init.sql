-- AgenticCP Core Database 초기화 스크립트

USE agenticcp;

-- 사용자 테이블 생성 (JPA가 자동으로 생성하지만 초기 데이터를 위해)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 초기 테스트 데이터 삽입
INSERT INTO users (username, email, name, is_active) VALUES
('admin', 'admin@agenticcp.com', '관리자', TRUE),
('testuser', 'test@agenticcp.com', '테스트 사용자', TRUE),
('developer', 'dev@agenticcp.com', '개발자', TRUE)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    name = VALUES(name),
    is_active = VALUES(is_active);

-- 인덱스 생성 (MySQL 8.0에서는 IF NOT EXISTS 지원 안함)
-- JPA가 자동으로 인덱스를 생성하므로 주석 처리
-- CREATE INDEX idx_users_username ON users(username);
-- CREATE INDEX idx_users_email ON users(email);
-- CREATE INDEX idx_users_active ON users(is_active);
-- CREATE INDEX idx_users_created_at ON users(created_at);

-- 감사 로그 조회 최적화 인덱스 (이력 조회 API p95 ≤ 200ms 목표)
-- 조건: resource_type = 'PlatformConfig' AND resource_id = :id AND event_type = 'CONFIGURATION_CHANGE'
-- 정렬: event_timestamp DESC
-- 참고: MySQL은 역순 정렬 최적화 시 DESC 인덱스가 도움될 수 있음
-- CREATE INDEX idx_audit_logs_rt_rid_et_ts_desc ON audit_logs(resource_type, resource_id, event_type, event_timestamp DESC);
-- 대안(버전 호환):
-- CREATE INDEX idx_audit_logs_rt_rid_et_ts ON audit_logs(resource_type, resource_id, event_type, event_timestamp);

-- 테넌트 설정 관련 테이블 생성
CREATE TABLE IF NOT EXISTS tenant_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    config_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'ENCRYPTED') NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_tenant_config (tenant_id, config_key),
    INDEX idx_tenant_config_tenant_id (tenant_id),
    INDEX idx_tenant_config_key (config_key),
    INDEX idx_tenant_config_deleted (is_deleted)
);

CREATE TABLE IF NOT EXISTS tenant_type_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_type ENUM('INDIVIDUAL', 'SMALL_BUSINESS', 'ENTERPRISE', 'GOVERNMENT') NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    config_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'ENCRYPTED') NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_tenant_type_config (tenant_type, config_key),
    INDEX idx_tenant_type_config_type (tenant_type),
    INDEX idx_tenant_type_config_key (config_key),
    INDEX idx_tenant_type_config_deleted (is_deleted)
);

-- 테넌트 타입별 기본 설정 데이터 삽입
INSERT INTO tenant_type_configs (tenant_type, config_key, config_value, config_type, description, is_encrypted) VALUES
-- ENTERPRISE 기본 설정
('ENTERPRISE', 'max_users', '1000', 'NUMBER', 'Maximum number of users for Enterprise tenants', FALSE),
('ENTERPRISE', 'max_storage_gb', '10000', 'NUMBER', 'Maximum storage quota in GB for Enterprise tenants', FALSE),
('ENTERPRISE', 'support_level', 'premium', 'STRING', 'Support level for Enterprise tenants', FALSE),
('ENTERPRISE', 'max_file_size', '1024', 'NUMBER', 'Maximum file size in MB for Enterprise tenants', FALSE),

-- SMALL_BUSINESS 기본 설정
('SMALL_BUSINESS', 'max_users', '100', 'NUMBER', 'Maximum number of users for Small Business tenants', FALSE),
('SMALL_BUSINESS', 'max_storage_gb', '1000', 'NUMBER', 'Maximum storage quota in GB for Small Business tenants', FALSE),
('SMALL_BUSINESS', 'support_level', 'standard', 'STRING', 'Support level for Small Business tenants', FALSE),
('SMALL_BUSINESS', 'max_file_size', '100', 'NUMBER', 'Maximum file size in MB for Small Business tenants', FALSE),

-- INDIVIDUAL 기본 설정
('INDIVIDUAL', 'max_users', '10', 'NUMBER', 'Maximum number of users for Individual tenants', FALSE),
('INDIVIDUAL', 'max_storage_gb', '100', 'NUMBER', 'Maximum storage quota in GB for Individual tenants', FALSE),
('INDIVIDUAL', 'support_level', 'basic', 'STRING', 'Support level for Individual tenants', FALSE),
('INDIVIDUAL', 'max_file_size', '50', 'NUMBER', 'Maximum file size in MB for Individual tenants', FALSE),

-- GOVERNMENT 기본 설정
('GOVERNMENT', 'max_users', '5000', 'NUMBER', 'Maximum number of users for Government tenants', FALSE),
('GOVERNMENT', 'max_storage_gb', '50000', 'NUMBER', 'Maximum storage quota in GB for Government tenants', FALSE),
('GOVERNMENT', 'support_level', 'government', 'STRING', 'Support level for Government tenants', FALSE),
('GOVERNMENT', 'max_file_size', '2048', 'NUMBER', 'Maximum file size in MB for Government tenants', FALSE)
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description);