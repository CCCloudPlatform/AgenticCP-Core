# Tenant Management Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    Tenant ||--o{ TenantConfig : "1:N"
    Tenant ||--o{ TenantBilling : "1:N"
    Tenant ||--|| TenantIsolation : "1:1"
    Tenant ||--o{ User : "1:N"
    Tenant ||--o{ Organization : "1:N"
    Tenant ||--o{ Role : "1:N"
    Tenant ||--o{ Permission : "1:N"
    Tenant ||--o{ CloudResource : "1:N"
    Tenant ||--o{ SecurityPolicy : "1:N"
```

## 주요 엔티티

### Tenant (테넌트)
```mermaid
erDiagram
    Tenant {
        bigint id PK "Primary Key"
        varchar tenant_key UK "테넌트 키 (Unique)"
        varchar tenant_name "테넌트명"
        text description "설명"
        enum status "상태"
        enum tenant_type "테넌트 타입"
        int max_users "최대 사용자 수"
        int max_resources "최대 리소스 수"
        bigint storage_quota_gb "스토리지 할당량 (GB)"
        bigint bandwidth_quota_gb "대역폭 할당량 (GB)"
        varchar contact_email "연락처 이메일"
        varchar contact_phone "연락처 전화번호"
        text billing_address "과금 주소"
        text settings "테넌트 설정 (JSON)"
        datetime subscription_start_date "구독 시작일"
        datetime subscription_end_date "구독 종료일"
        boolean is_trial "트라이얼 여부"
        datetime trial_end_date "트라이얼 종료일"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### TenantConfig (테넌트 설정)
```mermaid
erDiagram
    TenantConfig {
        bigint id PK "Primary Key"
        varchar config_key "설정 키"
        text config_value "설정 값"
        enum config_type "설정 타입"
        text default_value "기본값"
        text description "설명"
        boolean is_encrypted "암호화 여부"
        boolean is_required "필수 여부"
        bigint tenant_id FK "테넌트 ID"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### TenantBilling (테넌트 과금)
```mermaid
erDiagram
    TenantBilling {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        decimal base_amount "기본 금액"
        decimal usage_amount "사용량 금액"
        decimal discount_amount "할인 금액"
        decimal tax_amount "세금 금액"
        decimal total_amount "총 금액"
        enum billing_cycle "과금 주기"
        datetime billing_period_start "과금 기간 시작"
        datetime billing_period_end "과금 기간 종료"
        datetime due_date "납부 기한"
        datetime paid_date "납부일"
        varchar invoice_number "인보이스 번호"
        varchar payment_method "결제 방법"
        enum payment_status "결제 상태"
        varchar currency "통화"
        text notes "메모"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### TenantIsolation (테넌트 격리)
```mermaid
erDiagram
    TenantIsolation {
        bigint id PK "Primary Key"
        bigint tenant_id FK,UK "테넌트 ID (Unique)"
        enum isolation_level "격리 수준"
        boolean network_isolation "네트워크 격리"
        boolean data_isolation "데이터 격리"
        boolean compute_isolation "컴퓨트 격리"
        boolean storage_isolation "스토리지 격리"
        varchar vpc_id "VPC ID"
        text subnet_ids "서브넷 ID 목록 (JSON)"
        text security_group_ids "보안 그룹 ID 목록 (JSON)"
        varchar encryption_key_id "암호화 키 ID"
        int backup_retention_days "백업 보존 일수"
        text compliance_requirements "컴플라이언스 요구사항 (JSON)"
        text isolation_policies "격리 정책 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 열거형 (Enums)

### TenantType
```mermaid
erDiagram
    TenantType {
        INDIVIDUAL "개인"
        SMALL_BUSINESS "소규모 사업체"
        ENTERPRISE "기업"
        GOVERNMENT "정부기관"
    }
```

### BillingCycle
```mermaid
erDiagram
    BillingCycle {
        MONTHLY "월간"
        QUARTERLY "분기별"
        YEARLY "연간"
        USAGE_BASED "사용량 기반"
    }
```

### PaymentStatus
```mermaid
erDiagram
    PaymentStatus {
        PENDING "대기"
        PAID "결제완료"
        OVERDUE "연체"
        CANCELLED "취소"
        REFUNDED "환불"
    }
```

### IsolationLevel
```mermaid
erDiagram
    IsolationLevel {
        SHARED "공유"
        DEDICATED "전용"
        PRIVATE "프라이빗"
        GOVERNMENT "정부용"
    }
```

### ConfigType
```mermaid
erDiagram
    ConfigType {
        STRING "문자열"
        NUMBER "숫자"
        BOOLEAN "불린"
        JSON "JSON"
        ENCRYPTED "암호화"
        EMAIL "이메일"
        URL "URL"
    }
```

## 인덱스 전략

### Tenant 테이블
- `idx_tenant_key`: tenant_key 컬럼 (Unique)
- `idx_tenant_status`: status 컬럼
- `idx_tenant_type`: tenant_type 컬럼
- `idx_tenant_trial`: is_trial 컬럼

### TenantConfig 테이블
- `idx_tenant_config_tenant`: tenant_id 컬럼
- `idx_tenant_config_key`: config_key 컬럼
- `idx_tenant_config_type`: config_type 컬럼

### TenantBilling 테이블
- `idx_tenant_billing_tenant`: tenant_id 컬럼
- `idx_tenant_billing_status`: payment_status 컬럼
- `idx_tenant_billing_due_date`: due_date 컬럼
- `idx_tenant_billing_period`: (billing_period_start, billing_period_end) 복합

### TenantIsolation 테이블
- `idx_tenant_isolation_tenant`: tenant_id 컬럼 (Unique)
- `idx_tenant_isolation_level`: isolation_level 컬럼

## 비즈니스 규칙

1. **테넌트 격리**: 각 테넌트는 완전히 격리된 환경을 가짐
2. **설정 상속**: 테넌트 설정은 플랫폼 기본값을 상속받을 수 있음
3. **과금 모델**: 다양한 과금 주기와 결제 상태를 지원
4. **격리 수준**: 테넌트 요구사항에 따른 다양한 격리 수준 제공
5. **리소스 할당량**: 테넌트별 리소스 사용량 제한
6. **트라이얼 지원**: 신규 테넌트를 위한 트라이얼 기간 제공
