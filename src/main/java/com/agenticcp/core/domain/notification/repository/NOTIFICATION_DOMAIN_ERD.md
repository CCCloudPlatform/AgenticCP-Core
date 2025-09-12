# Notification & Communication Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    Tenant ||--o{ NotificationTemplate : "1:N"
    Tenant ||--o{ Notification : "1:N"
    Tenant ||--o{ NotificationChannel : "1:N"
    
    User ||--o{ Notification : "1:N"
    User ||--o{ NotificationPreference : "1:N"
    
    NotificationTemplate ||--o{ Notification : "1:N"
    NotificationChannel ||--o{ Notification : "1:N"
```

## 주요 엔티티

### NotificationTemplate (알림 템플릿)
```mermaid
erDiagram
    NotificationTemplate {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        varchar template_name "템플릿명"
        text description "설명"
        enum template_type "템플릿 타입"
        varchar subject "제목"
        text content "내용"
        text variables "변수 (JSON)"
        text metadata "메타데이터 (JSON)"
        boolean is_active "활성화 여부"
        enum status "상태"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Notification (알림)
```mermaid
erDiagram
    Notification {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint user_id FK "사용자 ID"
        bigint template_id FK "템플릿 ID"
        bigint channel_id FK "채널 ID"
        varchar notification_id UK "알림 ID (Unique)"
        varchar title "제목"
        text content "내용"
        enum notification_type "알림 타입"
        enum priority "우선순위"
        enum status "상태"
        text data "데이터 (JSON)"
        text metadata "메타데이터 (JSON)"
        datetime scheduled_at "예약 시간"
        datetime sent_at "발송 시간"
        datetime read_at "읽은 시간"
        text error_message "에러 메시지"
        int retry_count "재시도 횟수"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### NotificationChannel (알림 채널)
```mermaid
erDiagram
    NotificationChannel {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        varchar channel_name "채널명"
        text description "설명"
        enum channel_type "채널 타입"
        text configuration "설정 (JSON)"
        text credentials "인증 정보 (JSON)"
        boolean is_active "활성화 여부"
        enum status "상태"
        text metadata "메타데이터 (JSON)"
        datetime last_used "마지막 사용"
        int success_count "성공 횟수"
        int failure_count "실패 횟수"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### NotificationPreference (알림 설정)
```mermaid
erDiagram
    NotificationPreference {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint user_id FK "사용자 ID"
        enum notification_type "알림 타입"
        boolean email_enabled "이메일 활성화"
        boolean sms_enabled "SMS 활성화"
        boolean push_enabled "푸시 활성화"
        boolean webhook_enabled "웹훅 활성화"
        text email_addresses "이메일 주소 (JSON)"
        text phone_numbers "전화번호 (JSON)"
        text webhook_urls "웹훅 URL (JSON)"
        text preferences "설정 (JSON)"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### EmailService (이메일 서비스)
```mermaid
erDiagram
    EmailService {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        varchar service_name "서비스명"
        text description "설명"
        varchar smtp_host "SMTP 호스트"
        int smtp_port "SMTP 포트"
        varchar username "사용자명"
        varchar password "비밀번호"
        boolean use_tls "TLS 사용"
        boolean use_ssl "SSL 사용"
        varchar from_email "발신자 이메일"
        varchar from_name "발신자명"
        text headers "헤더 (JSON)"
        boolean is_active "활성화 여부"
        enum status "상태"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 열거형 (Enums)

### TemplateType
```mermaid
erDiagram
    TemplateType {
        EMAIL "이메일"
        SMS "SMS"
        PUSH "푸시"
        WEBHOOK "웹훅"
        IN_APP "앱 내"
    }
```

### NotificationType
```mermaid
erDiagram
    NotificationType {
        SYSTEM "시스템"
        SECURITY "보안"
        BILLING "과금"
        MAINTENANCE "유지보수"
        ALERT "알림"
        MARKETING "마케팅"
        CUSTOM "사용자 정의"
    }
```

### ChannelType
```mermaid
erDiagram
    ChannelType {
        EMAIL "이메일"
        SMS "SMS"
        PUSH "푸시"
        WEBHOOK "웹훅"
        SLACK "Slack"
        TEAMS "Microsoft Teams"
        DISCORD "Discord"
        CUSTOM "사용자 정의"
    }
```

### Priority
```mermaid
erDiagram
    Priority {
        LOW "낮음"
        MEDIUM "보통"
        HIGH "높음"
        URGENT "긴급"
    }
```

### NotificationStatus
```mermaid
erDiagram
    NotificationStatus {
        PENDING "대기"
        SENDING "발송중"
        SENT "발송완료"
        DELIVERED "전달완료"
        READ "읽음"
        FAILED "실패"
        CANCELLED "취소"
    }
```

## 인덱스 전략

### NotificationTemplate 테이블
- `idx_notification_template_tenant`: tenant_id 컬럼
- `idx_notification_template_name`: template_name 컬럼
- `idx_notification_template_type`: template_type 컬럼
- `idx_notification_template_status`: status 컬럼
- `idx_notification_template_active`: is_active 컬럼

### Notification 테이블
- `idx_notification_id`: notification_id 컬럼 (Unique)
- `idx_notification_tenant`: tenant_id 컬럼
- `idx_notification_user`: user_id 컬럼
- `idx_notification_type`: notification_type 컬럼
- `idx_notification_status`: status 컬럼
- `idx_notification_priority`: priority 컬럼
- `idx_notification_scheduled`: scheduled_at 컬럼
- `idx_notification_sent`: sent_at 컬럼
- `idx_notification_tenant_user`: (tenant_id, user_id) 복합

### NotificationChannel 테이블
- `idx_notification_channel_tenant`: tenant_id 컬럼
- `idx_notification_channel_name`: channel_name 컬럼
- `idx_notification_channel_type`: channel_type 컬럼
- `idx_notification_channel_status`: status 컬럼
- `idx_notification_channel_active`: is_active 컬럼

### NotificationPreference 테이블
- `idx_notification_preference_tenant`: tenant_id 컬럼
- `idx_notification_preference_user`: user_id 컬럼
- `idx_notification_preference_type`: notification_type 컬럼
- `idx_notification_preference_tenant_user`: (tenant_id, user_id) 복합

### EmailService 테이블
- `idx_email_service_tenant`: tenant_id 컬럼
- `idx_email_service_name`: service_name 컬럼
- `idx_email_service_status`: status 컬럼
- `idx_email_service_active`: is_active 컬럼

## 비즈니스 규칙

1. **템플릿 관리**: 재사용 가능한 알림 템플릿 시스템
2. **채널 지원**: 다양한 알림 채널 통합 관리
3. **사용자 설정**: 개인별 알림 설정 및 선호도 관리
4. **우선순위**: 알림 우선순위 기반 발송 관리
5. **예약 발송**: 미래 시점 알림 예약 기능
6. **재시도 정책**: 실패한 알림에 대한 자동 재시도
7. **상태 추적**: 알림 발송 상태의 완전한 추적
8. **개인화**: 사용자별 맞춤형 알림 내용
