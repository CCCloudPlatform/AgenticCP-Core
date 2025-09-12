# UI/UX Management Domain ERD

## 엔티티 관계도

```mermaid
erDiagram
    Tenant ||--o{ Theme : "1:N"
    Tenant ||--o{ Layout : "1:N"
    Tenant ||--o{ Component : "1:N"
    
    User ||--o{ UserPreference : "1:N"
    User ||--o{ Dashboard : "1:N"
    
    Theme ||--o{ Layout : "1:N"
    Layout ||--o{ Component : "1:N"
```

## 주요 엔티티

### Theme (테마)
```mermaid
erDiagram
    Theme {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        varchar theme_name "테마명"
        text description "설명"
        enum theme_type "테마 타입"
        enum status "상태"
        text color_scheme "색상 스키마 (JSON)"
        text typography "타이포그래피 (JSON)"
        text spacing "간격 (JSON)"
        text components "컴포넌트 (JSON)"
        text variables "변수 (JSON)"
        boolean is_default "기본 테마 여부"
        boolean is_public "공개 여부"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Layout (레이아웃)
```mermaid
erDiagram
    Layout {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint theme_id FK "테마 ID"
        varchar layout_name "레이아웃명"
        text description "설명"
        enum layout_type "레이아웃 타입"
        enum status "상태"
        text structure "구조 (JSON)"
        text grid_system "그리드 시스템 (JSON)"
        text breakpoints "브레이크포인트 (JSON)"
        text components "컴포넌트 (JSON)"
        text responsive "반응형 (JSON)"
        boolean is_default "기본 레이아웃 여부"
        boolean is_public "공개 여부"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Component (컴포넌트)
```mermaid
erDiagram
    Component {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint layout_id FK "레이아웃 ID"
        varchar component_name "컴포넌트명"
        text description "설명"
        enum component_type "컴포넌트 타입"
        enum status "상태"
        text properties "속성 (JSON)"
        text styles "스타일 (JSON)"
        text events "이벤트 (JSON)"
        text data_binding "데이터 바인딩 (JSON)"
        text validation "검증 (JSON)"
        text accessibility "접근성 (JSON)"
        boolean is_reusable "재사용 가능 여부"
        boolean is_public "공개 여부"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### UserPreference (사용자 설정)
```mermaid
erDiagram
    UserPreference {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint user_id FK "사용자 ID"
        varchar preference_key "설정 키"
        text preference_value "설정 값"
        enum preference_type "설정 타입"
        text description "설명"
        boolean is_encrypted "암호화 여부"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Dashboard (대시보드)
```mermaid
erDiagram
    Dashboard {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint user_id FK "사용자 ID"
        varchar dashboard_name "대시보드명"
        text description "설명"
        enum dashboard_type "대시보드 타입"
        enum status "상태"
        text layout "레이아웃 (JSON)"
        text widgets "위젯 (JSON)"
        text filters "필터 (JSON)"
        text settings "설정 (JSON)"
        boolean is_public "공개 여부"
        boolean is_default "기본 대시보드 여부"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

### Widget (위젯)
```mermaid
erDiagram
    Widget {
        bigint id PK "Primary Key"
        bigint tenant_id FK "테넌트 ID"
        bigint dashboard_id FK "대시보드 ID"
        varchar widget_name "위젯명"
        text description "설명"
        enum widget_type "위젯 타입"
        enum status "상태"
        text configuration "설정 (JSON)"
        text data_source "데이터 소스 (JSON)"
        text visualization "시각화 (JSON)"
        text filters "필터 (JSON)"
        text position "위치 (JSON)"
        text size "크기 (JSON)"
        boolean is_resizable "크기 조정 가능 여부"
        boolean is_movable "이동 가능 여부"
        text metadata "메타데이터 (JSON)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
        varchar created_by "생성자"
        varchar updated_by "수정자"
        boolean is_deleted "삭제여부"
    }
```

## 열거형 (Enums)

### ThemeType
```mermaid
erDiagram
    ThemeType {
        LIGHT "라이트"
        DARK "다크"
        HIGH_CONTRAST "고대비"
        CUSTOM "사용자 정의"
    }
```

### LayoutType
```mermaid
erDiagram
    LayoutType {
        SINGLE_COLUMN "단일 열"
        TWO_COLUMN "두 열"
        THREE_COLUMN "세 열"
        SIDEBAR "사이드바"
        HEADER_FOOTER "헤더 푸터"
        CUSTOM "사용자 정의"
    }
```

### ComponentType
```mermaid
erDiagram
    ComponentType {
        BUTTON "버튼"
        INPUT "입력"
        SELECT "선택"
        TABLE "테이블"
        CHART "차트"
        CARD "카드"
        MODAL "모달"
        NAVIGATION "네비게이션"
        CUSTOM "사용자 정의"
    }
```

### PreferenceType
```mermaid
erDiagram
    PreferenceType {
        THEME "테마"
        LANGUAGE "언어"
        TIMEZONE "시간대"
        LAYOUT "레이아웃"
        NOTIFICATION "알림"
        CUSTOM "사용자 정의"
    }
```

### DashboardType
```mermaid
erDiagram
    DashboardType {
        OVERVIEW "개요"
        INFRASTRUCTURE "인프라"
        APPLICATION "애플리케이션"
        SECURITY "보안"
        COST "비용"
        CUSTOM "사용자 정의"
    }
```

### WidgetType
```mermaid
erDiagram
    WidgetType {
        CHART "차트"
        TABLE "테이블"
        METRIC "메트릭"
        ALERT "알림"
        LOG "로그"
        CUSTOM "사용자 정의"
    }
```

## 인덱스 전략

### Theme 테이블
- `idx_theme_tenant`: tenant_id 컬럼
- `idx_theme_name`: theme_name 컬럼
- `idx_theme_type`: theme_type 컬럼
- `idx_theme_status`: status 컬럼
- `idx_theme_default`: is_default 컬럼
- `idx_theme_public`: is_public 컬럼

### Layout 테이블
- `idx_layout_tenant`: tenant_id 컬럼
- `idx_layout_theme`: theme_id 컬럼
- `idx_layout_name`: layout_name 컬럼
- `idx_layout_type`: layout_type 컬럼
- `idx_layout_status`: status 컬럼
- `idx_layout_default`: is_default 컬럼

### Component 테이블
- `idx_component_tenant`: tenant_id 컬럼
- `idx_component_layout`: layout_id 컬럼
- `idx_component_name`: component_name 컬럼
- `idx_component_type`: component_type 컬럼
- `idx_component_status`: status 컬럼
- `idx_component_reusable`: is_reusable 컬럼

### UserPreference 테이블
- `idx_user_preference_tenant`: tenant_id 컬럼
- `idx_user_preference_user`: user_id 컬럼
- `idx_user_preference_key`: preference_key 컬럼
- `idx_user_preference_type`: preference_type 컬럼
- `idx_user_preference_tenant_user`: (tenant_id, user_id) 복합

### Dashboard 테이블
- `idx_dashboard_tenant`: tenant_id 컬럼
- `idx_dashboard_user`: user_id 컬럼
- `idx_dashboard_name`: dashboard_name 컬럼
- `idx_dashboard_type`: dashboard_type 컬럼
- `idx_dashboard_status`: status 컬럼
- `idx_dashboard_public`: is_public 컬럼
- `idx_dashboard_default`: is_default 컬럼

### Widget 테이블
- `idx_widget_tenant`: tenant_id 컬럼
- `idx_widget_dashboard`: dashboard_id 컬럼
- `idx_widget_name`: widget_name 컬럼
- `idx_widget_type`: widget_type 컬럼
- `idx_widget_status`: status 컬럼

## 비즈니스 규칙

1. **테마 관리**: 일관된 UI/UX를 위한 테마 시스템
2. **레이아웃 관리**: 반응형 레이아웃 시스템
3. **컴포넌트 관리**: 재사용 가능한 UI 컴포넌트 라이브러리
4. **사용자 설정**: 개인화된 UI 설정 관리
5. **대시보드 관리**: 사용자 정의 대시보드 시스템
6. **위젯 관리**: 모듈화된 위젯 시스템
7. **접근성**: 웹 접근성 가이드라인 준수
8. **반응형**: 다양한 디바이스 지원
