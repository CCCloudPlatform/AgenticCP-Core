# Cloud Management Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    CloudProvider ||--o{ CloudService : "1:N"
    CloudProvider ||--o{ CloudRegion : "1:N"
    CloudProvider ||--o{ CloudResource : "1:N"
    
    CloudService ||--o{ CloudResource : "1:N"
    CloudRegion ||--o{ CloudResource : "1:N"
    
    Tenant ||--o{ CloudResource : "1:N"
```

## 주요 엔티티

### CloudProvider (클라우드 프로바이더)
```mermaid
erDiagram
    CloudProvider {
        bigint id PK "Primary Key"
        varchar provider_key UK "프로바이더 키 (Unique)"
        varchar provider_name "프로바이더명"
        text description "설명"
        enum provider_type "프로바이더 타입"
        enum status "상태"
        varchar api_endpoint "API 엔드포인트"
        varchar api_version "API 버전"
        enum authentication_type "인증 타입"
        text supported_regions "지원 리전 (JSON)"
        text supported_services "지원 서비스 (JSON)"
        enum pricing_model "가격 모델"
        boolean is_global "글로벌 여부"
        boolean is_government "정부용 여부"
        text compliance_certifications "컴플라이언스 인증 (JSON)"
        text metadata "메타데이터 (JSON)"
        datetime last_sync "마지막 동기화"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### CloudService (클라우드 서비스)
```mermaid
erDiagram
    CloudService {
        bigint id PK "Primary Key"
        varchar service_name "서비스명"
        text description "설명"
        varchar service_type "서비스 타입"
        enum status "상태"
        bigint provider_id FK "프로바이더 ID"
        varchar api_version "API 버전"
        text supported_operations "지원 작업 (JSON)"
        text pricing_info "가격 정보 (JSON)"
        text service_limits "서비스 제한 (JSON)"
        text metadata "메타데이터 (JSON)"
        boolean is_available "사용 가능 여부"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### CloudRegion (클라우드 리전)
```mermaid
erDiagram
    CloudRegion {
        bigint id PK "Primary Key"
        varchar region_name "리전명"
        varchar region_code "리전 코드"
        text description "설명"
        bigint provider_id FK "프로바이더 ID"
        enum status "상태"
        varchar location "위치"
        varchar timezone "시간대"
        boolean is_available "사용 가능 여부"
        text supported_services "지원 서비스 (JSON)"
        text pricing_info "가격 정보 (JSON)"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### CloudResource (클라우드 리소스)
```mermaid
erDiagram
    CloudResource {
        bigint id PK "Primary Key"
        varchar resource_id UK "리소스 ID (Unique)"
        varchar resource_name "리소스명"
        text description "설명"
        bigint provider_id FK "프로바이더 ID"
        bigint service_id FK "서비스 ID"
        bigint region_id FK "리전 ID"
        bigint tenant_id FK "테넌트 ID"
        varchar resource_type "리소스 타입"
        enum status "상태"
        text configuration "설정 (JSON)"
        text tags "태그 (JSON)"
        text metadata "메타데이터 (JSON)"
        decimal cost_per_hour "시간당 비용"
        text monitoring_config "모니터링 설정 (JSON)"
        datetime last_updated "마지막 업데이트"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 열거형 (Enums)

### ProviderType
```mermaid
erDiagram
    ProviderType {
        AWS "Amazon Web Services"
        AZURE "Microsoft Azure"
        GCP "Google Cloud Platform"
        ALIBABA_CLOUD "Alibaba Cloud"
        IBM_CLOUD "IBM Cloud"
        ORACLE_CLOUD "Oracle Cloud"
        VMWARE "VMware"
        OPENSTACK "OpenStack"
        KUBERNETES "Kubernetes"
        DOCKER "Docker"
    }
```

### AuthenticationType
```mermaid
erDiagram
    AuthenticationType {
        API_KEY "API 키"
        OAUTH2 "OAuth 2.0"
        IAM_ROLE "IAM 역할"
        SERVICE_ACCOUNT "서비스 계정"
        CERTIFICATE "인증서"
        TOKEN "토큰"
    }
```

### PricingModel
```mermaid
erDiagram
    PricingModel {
        PAY_AS_YOU_GO "종량제"
        RESERVED_INSTANCE "예약 인스턴스"
        SPOT_INSTANCE "스팟 인스턴스"
        SAVINGS_PLANS "절약 플랜"
        COMMITTED_USE "커밋 사용"
        PREPAID "선불"
    }
```

## 인덱스 전략

### CloudProvider 테이블
- `idx_cloud_provider_key`: provider_key 컬럼 (Unique)
- `idx_cloud_provider_type`: provider_type 컬럼
- `idx_cloud_provider_status`: status 컬럼
- `idx_cloud_provider_global`: is_global 컬럼
- `idx_cloud_provider_government`: is_government 컬럼

### CloudService 테이블
- `idx_cloud_service_provider`: provider_id 컬럼
- `idx_cloud_service_type`: service_type 컬럼
- `idx_cloud_service_status`: status 컬럼
- `idx_cloud_service_available`: is_available 컬럼

### CloudRegion 테이블
- `idx_cloud_region_provider`: provider_id 컬럼
- `idx_cloud_region_code`: region_code 컬럼
- `idx_cloud_region_status`: status 컬럼
- `idx_cloud_region_available`: is_available 컬럼

### CloudResource 테이블
- `idx_cloud_resource_id`: resource_id 컬럼 (Unique)
- `idx_cloud_resource_provider`: provider_id 컬럼
- `idx_cloud_resource_service`: service_id 컬럼
- `idx_cloud_resource_region`: region_id 컬럼
- `idx_cloud_resource_tenant`: tenant_id 컬럼
- `idx_cloud_resource_type`: resource_type 컬럼
- `idx_cloud_resource_status`: status 컬럼

## 비즈니스 규칙

1. **프로바이더 관리**: 다양한 클라우드 프로바이더를 통합 관리
2. **서비스 추상화**: 프로바이더별 서비스를 통합된 인터페이스로 제공
3. **리전 지원**: 글로벌 리전별 리소스 관리
4. **테넌트 격리**: 테넌트별 리소스 격리 및 관리
5. **비용 추적**: 리소스별 비용 모니터링 및 관리
6. **상태 관리**: 리소스의 생명주기 상태 추적
7. **메타데이터**: 리소스별 상세 정보 및 설정 저장
8. **태그 관리**: 리소스 분류 및 관리를 위한 태그 시스템
