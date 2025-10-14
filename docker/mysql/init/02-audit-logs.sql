-- 감사 로그 테이블 생성 스크립트

USE agenticcp;

-- 감사 로그 테이블
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 감사 정보
    action VARCHAR(100) NOT NULL COMMENT '수행된 액션',
    resource_type VARCHAR(50) NOT NULL COMMENT '리소스 타입',
    http_method VARCHAR(10) COMMENT 'HTTP 메소드',
    request_path VARCHAR(500) COMMENT '요청 경로',
    operation_summary VARCHAR(200) COMMENT '작업 요약',
    controller_name VARCHAR(100) COMMENT '컨트롤러명',
    method_name VARCHAR(100) COMMENT '메소드명',
    severity VARCHAR(20) NOT NULL COMMENT '심각도',
    
    -- 타임스탬프
    timestamp DATETIME(6) NOT NULL COMMENT '이벤트 발생 시각',
    
    -- 컨텍스트 정보
    request_id VARCHAR(100) COMMENT '요청 ID',
    tenant_id VARCHAR(100) COMMENT '테넌트 ID',
    user_id VARCHAR(100) COMMENT '사용자 ID',
    client_ip VARCHAR(50) COMMENT '클라이언트 IP',
    
    -- 실행 결과
    success BOOLEAN NOT NULL COMMENT '성공 여부',
    error TEXT COMMENT '에러 메시지',
    
    -- 데이터 (JSON)
    request_data JSON COMMENT '요청 데이터',
    response_data JSON COMMENT '응답 데이터',
    metadata JSON COMMENT '메타데이터',
    
    -- 값 변경 추적 필드
    old_value JSON COMMENT '변경 전 값',
    new_value JSON COMMENT '변경 후 값',
    target_resource_id VARCHAR(100) COMMENT '변경 대상 리소스 ID',
    
    -- BaseEntity 필드
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    updated_by VARCHAR(100) COMMENT '수정자',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '삭제 여부',
    
    -- 인덱스
    INDEX idx_audit_logs_timestamp (timestamp),
    INDEX idx_audit_logs_tenant (tenant_id),
    INDEX idx_audit_logs_user (user_id),
    INDEX idx_audit_logs_action (action),
    INDEX idx_audit_logs_resource_type (resource_type),
    INDEX idx_audit_logs_severity (severity),
    INDEX idx_audit_logs_success (success),
    INDEX idx_audit_logs_request_id (request_id),
    INDEX idx_audit_logs_created_at (created_at),
    INDEX idx_audit_logs_target_resource_id (target_resource_id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='감사 로그 테이블';


