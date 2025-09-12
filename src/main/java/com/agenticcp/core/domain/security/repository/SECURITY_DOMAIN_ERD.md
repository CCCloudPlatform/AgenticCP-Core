# Security & Compliance Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    Tenant ||--o{ SecurityPolicy : "1:N"
    Tenant ||--o{ ThreatDetection : "1:N"
    Tenant ||--o{ Compliance : "1:N"
    Tenant ||--o{ AuditLog : "1:N"
    
    User ||--o{ AuditLog : "1:N"
```

## 주요 엔티티

### SecurityPolicy (보안 정책)
```mermaid
erDiagram
    SecurityPolicy {
        bigint id PK "Primary Key"
        varchar policy_key UK "정책 키 (Unique)"
        varchar policy_name "정책명"
        text description "설명"
        bigint tenant_id FK "테넌트 ID"
        enum status "상태"
        enum policy_type "정책 타입"
        enum severity "심각도"
        boolean is_global "글로벌 정책 여부"
        boolean is_system "시스템 정책 여부"
        boolean is_enabled "활성화 여부"
        text rules "규칙 (JSON)"
        text conditions "조건 (JSON)"
        text actions "액션 (JSON)"
        text target_resources "대상 리소스 (JSON)"
        text exceptions "예외 (JSON)"
        datetime effective_from "유효 시작일"
        datetime effective_until "유효 종료일"
        int priority "우선순위"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### ThreatDetection (위협 탐지)
```mermaid
erDiagram
    ThreatDetection {
        bigint id PK "Primary Key"
        varchar threat_id UK "위협 ID (Unique)"
        varchar threat_name "위협명"
        text description "설명"
        bigint tenant_id FK "테넌트 ID"
        enum status "상태"
        enum threat_type "위협 타입"
        enum severity "심각도"
        enum confidence_level "신뢰도 수준"
        boolean is_active "활성화 여부"
        boolean is_auto_remediate "자동 대응 여부"
        text detection_rules "탐지 규칙 (JSON)"
        text indicators "지표 (JSON)"
        text mitigation_actions "완화 액션 (JSON)"
        text exceptions "예외 (JSON)"
        text target_resources "대상 리소스 (JSON)"
        text metadata "메타데이터 (JSON)"
        bigint detection_count "탐지 횟수"
        bigint true_positive_count "진양성 횟수"
        bigint false_positive_count "거짓양성 횟수"
        datetime last_detected "마지막 탐지일"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Compliance (컴플라이언스)
```mermaid
erDiagram
    Compliance {
        bigint id PK "Primary Key"
        varchar compliance_key UK "컴플라이언스 키 (Unique)"
        varchar compliance_name "컴플라이언스명"
        text description "설명"
        bigint tenant_id FK "테넌트 ID"
        enum status "상태"
        varchar standard_name "표준명"
        varchar version "버전"
        text requirements "요구사항 (JSON)"
        text controls "통제 (JSON)"
        text evidence "증거 (JSON)"
        text remediation "구제책 (JSON)"
        datetime assessment_date "평가일"
        datetime next_assessment "다음 평가일"
        text assessor "평가자"
        text notes "메모"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### AuditLog (감사 로그)
```mermaid
erDiagram
    AuditLog {
        bigint id PK "Primary Key"
        varchar event_id UK "이벤트 ID (Unique)"
        varchar event_type "이벤트 타입"
        text description "설명"
        bigint user_id FK "사용자 ID"
        bigint tenant_id FK "테넌트 ID"
        varchar resource_type "리소스 타입"
        varchar resource_id "리소스 ID"
        varchar action "액션"
        text old_values "이전 값 (JSON)"
        text new_values "새 값 (JSON)"
        varchar ip_address "IP 주소"
        varchar user_agent "사용자 에이전트"
        varchar session_id "세션 ID"
        text metadata "메타데이터 (JSON)"
        datetime event_time "이벤트 시간"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 열거형 (Enums)

### PolicyType
```mermaid
erDiagram
    PolicyType {
        ACCESS_CONTROL "접근 제어"
        DATA_PROTECTION "데이터 보호"
        NETWORK_SECURITY "네트워크 보안"
        ENCRYPTION "암호화"
        AUTHENTICATION "인증"
        AUTHORIZATION "인가"
        AUDIT_LOGGING "감사 로깅"
        INCIDENT_RESPONSE "사고 대응"
        BACKUP_RECOVERY "백업 복구"
        COMPLIANCE "컴플라이언스"
        VULNERABILITY_MANAGEMENT "취약점 관리"
        THREAT_DETECTION "위협 탐지"
    }
```

### ThreatType
```mermaid
erDiagram
    ThreatType {
        MALWARE "악성코드"
        PHISHING "피싱"
        RANSOMWARE "랜섬웨어"
        DDoS "DDoS 공격"
        SQL_INJECTION "SQL 인젝션"
        XSS "XSS 공격"
        CSRF "CSRF 공격"
        BRUTE_FORCE "무차별 대입"
        UNAUTHORIZED_ACCESS "무단 접근"
        DATA_BREACH "데이터 유출"
        DATA_EXFILTRATION "데이터 반출"
        INSIDER_THREAT "내부자 위협"
        APT "APT 공격"
        BOTNET "봇넷"
        CRYPTOCURRENCY_MINING "암호화폐 채굴"
        CONFIGURATION_DRIFT "설정 변경"
        VULNERABILITY_EXPLOIT "취약점 악용"
        ACCOUNT_TAKEOVER "계정 탈취"
        PRIVILEGE_ESCALATION "권한 상승"
        CUSTOM "사용자 정의"
    }
```

### Severity
```mermaid
erDiagram
    Severity {
        LOW "낮음"
        MEDIUM "보통"
        HIGH "높음"
        CRITICAL "치명적"
    }
```

### ConfidenceLevel
```mermaid
erDiagram
    ConfidenceLevel {
        LOW "낮음"
        MEDIUM "보통"
        HIGH "높음"
        VERY_HIGH "매우 높음"
    }
```

## 인덱스 전략

### SecurityPolicy 테이블
- `idx_security_policy_key`: policy_key 컬럼 (Unique)
- `idx_security_policy_tenant`: tenant_id 컬럼
- `idx_security_policy_type`: policy_type 컬럼
- `idx_security_policy_status`: status 컬럼
- `idx_security_policy_global`: is_global 컬럼
- `idx_security_policy_system`: is_system 컬럼
- `idx_security_policy_enabled`: is_enabled 컬럼
- `idx_security_policy_priority`: priority 컬럼

### ThreatDetection 테이블
- `idx_threat_detection_id`: threat_id 컬럼 (Unique)
- `idx_threat_detection_tenant`: tenant_id 컬럼
- `idx_threat_detection_type`: threat_type 컬럼
- `idx_threat_detection_status`: status 컬럼
- `idx_threat_detection_severity`: severity 컬럼
- `idx_threat_detection_active`: is_active 컬럼
- `idx_threat_detection_last_detected`: last_detected 컬럼

### Compliance 테이블
- `idx_compliance_key`: compliance_key 컬럼 (Unique)
- `idx_compliance_tenant`: tenant_id 컬럼
- `idx_compliance_standard`: standard_name 컬럼
- `idx_compliance_status`: status 컬럼
- `idx_compliance_assessment`: assessment_date 컬럼

### AuditLog 테이블
- `idx_audit_log_event_id`: event_id 컬럼 (Unique)
- `idx_audit_log_user`: user_id 컬럼
- `idx_audit_log_tenant`: tenant_id 컬럼
- `idx_audit_log_event_type`: event_type 컬럼
- `idx_audit_log_event_time`: event_time 컬럼
- `idx_audit_log_resource`: (resource_type, resource_id) 복합
- `idx_audit_log_action`: action 컬럼

## 비즈니스 규칙

1. **정책 우선순위**: 정책은 우선순위에 따라 적용됨
2. **글로벌 정책**: 시스템 전체에 적용되는 글로벌 정책 지원
3. **테넌트 격리**: 테넌트별 보안 정책 격리
4. **위협 탐지**: 실시간 위협 탐지 및 대응
5. **컴플라이언스**: 다양한 보안 표준 준수 관리
6. **감사 추적**: 모든 보안 관련 활동의 완전한 감사 로그
7. **자동 대응**: 위협 탐지 시 자동 대응 메커니즘
8. **정책 버전 관리**: 정책의 변경 이력 추적
