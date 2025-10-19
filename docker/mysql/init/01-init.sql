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