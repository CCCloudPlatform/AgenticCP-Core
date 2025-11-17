-- Organization-Role mapping table
CREATE TABLE IF NOT EXISTS organization_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    priority INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_org_roles_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_org_roles_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT uk_organization_role UNIQUE (organization_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_org_roles_org ON organization_roles(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_roles_role ON organization_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_org_roles_default ON organization_roles(is_default);
CREATE INDEX IF NOT EXISTS idx_org_roles_priority ON organization_roles(priority);


