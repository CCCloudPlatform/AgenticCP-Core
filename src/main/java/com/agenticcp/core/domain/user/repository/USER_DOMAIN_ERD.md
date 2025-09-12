# User & Access Management Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    Tenant ||--o{ User : "1:N"
    Tenant ||--o{ Organization : "1:N"
    Tenant ||--o{ Role : "1:N"
    Tenant ||--o{ Permission : "1:N"
    
    Organization ||--o{ User : "1:N"
    Organization ||--o{ Organization : "1:N (Self-Reference)"
    
    User ||--o{ UserRole : "M:N"
    User ||--o{ UserPermission : "M:N"
    
    Role ||--o{ UserRole : "M:N"
    Role ||--o{ RolePermission : "M:N"
    
    Permission ||--o{ UserPermission : "M:N"
    Permission ||--o{ RolePermission : "M:N"
```

## 주요 엔티티

### User (사용자)
```mermaid
erDiagram
    User {
        bigint id PK "Primary Key"
        varchar username UK "사용자명 (Unique)"
        varchar email UK "이메일 (Unique)"
        varchar name "실명"
        varchar password_hash "비밀번호 해시"
        bigint tenant_id FK "테넌트 ID"
        bigint organization_id FK "조직 ID"
        enum role "사용자 역할"
        enum status "상태"
        datetime last_login "마지막 로그인"
        int failed_login_attempts "실패 로그인 횟수"
        datetime locked_until "잠금 해제 시간"
        datetime password_changed_at "비밀번호 변경일"
        boolean two_factor_enabled "2FA 활성화"
        varchar two_factor_secret "2FA 시크릿"
        varchar profile_image_url "프로필 이미지 URL"
        varchar phone_number "전화번호"
        varchar department "부서"
        varchar job_title "직책"
        varchar timezone "시간대"
        varchar language "언어"
        text preferences "사용자 설정 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Organization (조직)
```mermaid
erDiagram
    Organization {
        bigint id PK "Primary Key"
        varchar org_key UK "조직 키 (Unique)"
        varchar org_name "조직명"
        text description "설명"
        bigint tenant_id FK "테넌트 ID"
        bigint parent_org_id FK "상위 조직 ID"
        enum status "상태"
        enum org_type "조직 타입"
        varchar contact_email "연락처 이메일"
        varchar contact_phone "연락처 전화번호"
        text address "주소"
        varchar website "웹사이트"
        int max_users "최대 사용자 수"
        text settings "조직 설정 (JSON)"
        datetime established_date "설립일"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Role (역할)
```mermaid
erDiagram
    Role {
        bigint id PK "Primary Key"
        varchar role_key "역할 키"
        varchar role_name "역할명"
        text description "설명"
        bigint tenant_id FK "테넌트 ID"
        enum status "상태"
        boolean is_system "시스템 역할 여부"
        boolean is_default "기본 역할 여부"
        int priority "우선순위"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Permission (권한)
```mermaid
erDiagram
    Permission {
        bigint id PK "Primary Key"
        varchar permission_key "권한 키"
        varchar permission_name "권한명"
        text description "설명"
        bigint tenant_id FK "테넌트 ID"
        enum status "상태"
        varchar resource "리소스"
        varchar action "액션"
        boolean is_system "시스템 권한 여부"
        varchar category "카테고리"
        int priority "우선순위"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 중간 테이블

### UserRole (사용자-역할 매핑)
```mermaid
erDiagram
    UserRole {
        bigint user_id PK,FK "사용자 ID"
        bigint role_id PK,FK "역할 ID"
    }
```

### UserPermission (사용자-권한 매핑)
```mermaid
erDiagram
    UserPermission {
        bigint user_id PK,FK "사용자 ID"
        bigint permission_id PK,FK "권한 ID"
    }
```

### RolePermission (역할-권한 매핑)
```mermaid
erDiagram
    RolePermission {
        bigint role_id PK,FK "역할 ID"
        bigint permission_id PK,FK "권한 ID"
    }
```

## 열거형 (Enums)

### UserRole
```mermaid
erDiagram
    UserRole {
        SUPER_ADMIN "최고관리자"
        TENANT_ADMIN "테넌트관리자"
        CLOUD_ADMIN "클라우드관리자"
        DEVELOPER "개발자"
        VIEWER "조회자"
        AUDITOR "감사자"
    }
```

### OrganizationType
```mermaid
erDiagram
    OrganizationType {
        COMPANY "회사"
        DEPARTMENT "부서"
        TEAM "팀"
        PROJECT "프로젝트"
        DIVISION "사업부"
    }
```

## 인덱스 전략

### User 테이블
- `idx_users_username`: username 컬럼
- `idx_users_email`: email 컬럼
- `idx_users_tenant`: tenant_id 컬럼
- `idx_users_active`: status 컬럼
- `idx_users_organization`: organization_id 컬럼

### Organization 테이블
- `idx_org_tenant`: tenant_id 컬럼
- `idx_org_parent`: parent_org_id 컬럼
- `idx_org_status`: status 컬럼

### Role 테이블
- `idx_role_tenant`: tenant_id 컬럼
- `idx_role_system`: is_system 컬럼
- `idx_role_default`: is_default 컬럼

### Permission 테이블
- `idx_permission_tenant`: tenant_id 컬럼
- `idx_permission_resource`: resource 컬럼
- `idx_permission_action`: action 컬럼

## 비즈니스 규칙

1. **사용자-테넌트 관계**: 모든 사용자는 반드시 하나의 테넌트에 속해야 함
2. **조직 계층 구조**: 조직은 자기 참조로 계층 구조를 가질 수 있음
3. **권한 상속**: 사용자는 역할을 통해 권한을 상속받을 수 있음
4. **직접 권한**: 사용자는 역할과 별개로 직접 권한을 가질 수 있음
5. **테넌트 격리**: 모든 엔티티는 테넌트별로 격리됨
