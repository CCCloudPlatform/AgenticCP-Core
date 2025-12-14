-- Worker 기반 멀티 테넌트 구조 마이그레이션 스크립트
-- 이슈 #172: Organization Business 로직 리팩토링 (설계 B 기준)
-- 
-- 주의사항:
-- 1. 마이그레이션 전 반드시 데이터 백업
-- 2. 단계별 검증 후 다음 단계 진행
-- 3. 롤백 스크립트 준비 권장

USE agenticcp;

-- ========== 1. 새로운 테이블 생성 ==========

-- Worker 테이블 (설계 B 기준)
-- User 1:N Worker 관계, (user_id, tenant_id) 복합 Unique 제약
CREATE TABLE IF NOT EXISTS workers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_worker_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_worker_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_worker_user_tenant UNIQUE (user_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_worker_user ON workers(user_id);
CREATE INDEX IF NOT EXISTS idx_worker_tenant ON workers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_worker_deleted ON workers(is_deleted);

-- OrganizationMember 테이블 (복합 PK)
-- (organization_id, user_id)가 복합 PK
CREATE TABLE IF NOT EXISTS organization_member (
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (organization_id, user_id),
    CONSTRAINT fk_org_member_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_org_member_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_org_member_org ON organization_member(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_member_user ON organization_member(user_id);

-- TenantWorkerMap 테이블 (복합 PK)
-- Shared Tenant 접근 관리
CREATE TABLE IF NOT EXISTS tenant_worker_map (
    tenant_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,
    access_scope VARCHAR(30),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (tenant_id, worker_id),
    CONSTRAINT fk_tenant_worker_map_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_worker_map_worker FOREIGN KEY (worker_id) REFERENCES workers(id)
);

CREATE INDEX IF NOT EXISTS idx_tenant_worker_map_tenant ON tenant_worker_map(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_worker_map_worker ON tenant_worker_map(worker_id);
CREATE INDEX IF NOT EXISTS idx_tenant_worker_map_deleted ON tenant_worker_map(is_deleted);

-- WorkerRole 테이블 (복합 PK)
-- Worker 역할 관리: (worker_id, role_id, tenant_id)가 복합 PK
CREATE TABLE IF NOT EXISTS worker_role (
    worker_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (worker_id, role_id, tenant_id),
    CONSTRAINT fk_worker_role_worker FOREIGN KEY (worker_id) REFERENCES workers(id),
    CONSTRAINT fk_worker_role_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_worker_role_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_worker_role_worker ON worker_role(worker_id);
CREATE INDEX IF NOT EXISTS idx_worker_role_tenant ON worker_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_worker_role_role ON worker_role(role_id);
CREATE INDEX IF NOT EXISTS idx_worker_role_deleted ON worker_role(is_deleted);

-- ========== 2. Tenant 테이블 수정 ==========
-- 설계 B: Organization ↔ Tenant 1:1 관계
-- tenant_type은 이미 존재하므로 유지
-- owner_org_id는 제거 (1:1 관계로 organization_id로 관리)

-- tenant_type 컬럼이 없으면 추가
-- MySQL 8.0.19 이전 버전 호환을 위해 프로시저 사용
SET @dbname = DATABASE();
SET @tablename = 'tenants';
SET @columnname = 'tenant_type';
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = @tablename)
            AND (COLUMN_NAME = @columnname)
    ) > 0,
    'SELECT 1',
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) CHECK (tenant_type IN (''DEDICATED'',''SHARED''))')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ========== 3. 기존 데이터 마이그레이션 ==========

-- 3.1. User.tenant_id를 기반으로 Worker 생성
-- User 1:N Worker 관계 구현
-- 주의: User.tenant_id가 NULL인 경우는 제외
INSERT INTO workers (user_id, tenant_id, created_at, updated_at, created_by)
SELECT 
    id AS user_id,
    tenant_id,
    created_at,
    updated_at,
    created_by
FROM users
WHERE tenant_id IS NOT NULL
ON DUPLICATE KEY UPDATE 
    updated_at = CURRENT_TIMESTAMP;

-- 3.2. User.organization_id를 organization_member로 이관
-- 주의: User.organization_id가 NULL인 경우는 제외
INSERT INTO organization_member (organization_id, user_id, role, joined_at, created_at, updated_at)
SELECT 
    organization_id,
    id AS user_id,
    NULL AS role,  -- 기존 데이터에 role 정보가 없으므로 NULL
    created_at AS joined_at,
    created_at,
    updated_at
FROM users
WHERE organization_id IS NOT NULL
ON DUPLICATE KEY UPDATE 
    updated_at = CURRENT_TIMESTAMP;

-- 3.3. Dedicated Tenant의 경우 Worker가 자동으로 소속되므로 별도 작업 불필요
-- Shared Tenant의 경우 TenantWorkerMap은 수동으로 할당해야 함

-- ========== 4. 데이터 검증 쿼리 ==========

-- Worker 생성 확인
SELECT 
    'Workers created' AS status,
    COUNT(*) AS count
FROM workers;

-- OrganizationMember 생성 확인
SELECT 
    'OrganizationMembers created' AS status,
    COUNT(*) AS count
FROM organization_member;

-- User별 Worker 수 확인
SELECT 
    u.id AS user_id,
    u.username,
    COUNT(w.id) AS worker_count
FROM users u
LEFT JOIN workers w ON u.id = w.user_id
GROUP BY u.id, u.username
ORDER BY worker_count DESC;

-- Tenant별 Worker 수 확인
SELECT 
    t.id AS tenant_id,
    t.tenant_key,
    t.tenant_type,
    COUNT(w.id) AS worker_count
FROM tenants t
LEFT JOIN workers w ON t.id = w.tenant_id
GROUP BY t.id, t.tenant_key, t.tenant_type
ORDER BY worker_count DESC;

-- ========== 5. 롤백 스크립트 (참고용) ==========
-- 주의: 실제 롤백 시에는 데이터 백업에서 복원하는 것을 권장

/*
-- 롤백 순서 (역순)
DROP TABLE IF EXISTS worker_role;
DROP TABLE IF EXISTS tenant_worker_map;
DROP TABLE IF EXISTS organization_member;
DROP TABLE IF EXISTS workers;
*/

