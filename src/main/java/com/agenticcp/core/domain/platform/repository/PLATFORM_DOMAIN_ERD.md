# Platform Management Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    PlatformConfig ||--o{ PlatformHealth : "1:N (간접적)"
    FeatureFlag ||--o{ PlatformHealth : "1:N (간접적)"
    License ||--o{ PlatformHealth : "1:N (간접적)"
```

## 주요 엔티티

### PlatformConfig (플랫폼 설정)
```mermaid
erDiagram
    PlatformConfig {
        bigint id PK "Primary Key"
        varchar config_key UK "설정 키 (Unique)"
        text config_value "설정 값"
        enum config_type "설정 타입"
        text description "설명"
        boolean is_encrypted "암호화 여부"
        boolean is_system "시스템 설정 여부"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### FeatureFlag (기능 플래그)
```mermaid
erDiagram
    FeatureFlag {
        bigint id PK "Primary Key"
        varchar flag_key UK "플래그 키 (Unique)"
        varchar flag_name "플래그명"
        text description "설명"
        boolean is_enabled "활성화 여부"
        enum status "상태"
        text target_tenants "대상 테넌트 (JSON)"
        text target_users "대상 사용자 (JSON)"
        int rollout_percentage "롤아웃 비율"
        datetime start_date "시작일"
        datetime end_date "종료일"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### License (라이선스)
```mermaid
erDiagram
    License {
        bigint id PK "Primary Key"
        varchar license_key UK "라이선스 키 (Unique)"
        varchar license_name "라이선스명"
        text description "설명"
        enum status "상태"
        varchar license_type "라이선스 타입"
        varchar version "버전"
        int max_users "최대 사용자 수"
        int max_tenants "최대 테넌트 수"
        int max_resources "최대 리소스 수"
        datetime start_date "시작일"
        datetime end_date "종료일"
        boolean is_trial "트라이얼 여부"
        text features "기능 목록 (JSON)"
        text restrictions "제한사항 (JSON)"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### PlatformHealth (플랫폼 상태)
```mermaid
erDiagram
    PlatformHealth {
        bigint id PK "Primary Key"
        varchar service_name "서비스명"
        enum status "상태"
        float cpu_usage_percent "CPU 사용률 (%)"
        float memory_usage_percent "메모리 사용률 (%)"
        float disk_usage_percent "디스크 사용률 (%)"
        bigint response_time_ms "응답 시간 (ms)"
        bigint error_count "에러 수"
        varchar error_message "에러 메시지"
        datetime last_check_time "마지막 체크 시간"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 열거형 (Enums)

### ConfigType
```mermaid
erDiagram
    ConfigType {
        STRING "문자열"
        NUMBER "숫자"
        BOOLEAN "불린"
        JSON "JSON"
        ENCRYPTED "암호화"
    }
```

### LicenseType
```mermaid
erDiagram
    LicenseType {
        TRIAL "트라이얼"
        BASIC "기본"
        PROFESSIONAL "프로페셔널"
        ENTERPRISE "엔터프라이즈"
        GOVERNMENT "정부용"
        CUSTOM "사용자 정의"
    }
```

### HealthStatus
```mermaid
erDiagram
    HealthStatus {
        HEALTHY "정상"
        WARNING "경고"
        CRITICAL "치명적"
        UNKNOWN "알 수 없음"
    }
```

## 인덱스 전략

### PlatformConfig 테이블
- `idx_platform_config_key`: config_key 컬럼 (Unique)
- `idx_platform_config_type`: config_type 컬럼
- `idx_platform_config_system`: is_system 컬럼
- `idx_platform_config_encrypted`: is_encrypted 컬럼

### FeatureFlag 테이블
- `idx_feature_flag_key`: flag_key 컬럼 (Unique)
- `idx_feature_flag_status`: status 컬럼
- `idx_feature_flag_enabled`: is_enabled 컬럼
- `idx_feature_flag_rollout`: rollout_percentage 컬럼
- `idx_feature_flag_dates`: (start_date, end_date) 복합

### License 테이블
- `idx_license_key`: license_key 컬럼 (Unique)
- `idx_license_type`: license_type 컬럼
- `idx_license_status`: status 컬럼
- `idx_license_trial`: is_trial 컬럼
- `idx_license_dates`: (start_date, end_date) 복합

### PlatformHealth 테이블
- `idx_platform_health_service`: service_name 컬럼
- `idx_platform_health_status`: status 컬럼
- `idx_platform_health_check_time`: last_check_time 컬럼
- `idx_platform_health_created`: created_at 컬럼

## 비즈니스 규칙

1. **설정 관리**: 플랫폼 전역 설정의 중앙화된 관리
2. **기능 플래그**: 동적 기능 제어 및 A/B 테스트 지원
3. **라이선스 관리**: 라이선스 검증 및 기능 제한
4. **상태 모니터링**: 실시간 플랫폼 상태 추적
5. **암호화 설정**: 민감한 설정의 암호화 저장
6. **롤아웃 제어**: 점진적 기능 배포 지원
7. **메타데이터**: 설정 및 기능에 대한 상세 정보 저장
8. **버전 관리**: 설정 변경 이력 추적
