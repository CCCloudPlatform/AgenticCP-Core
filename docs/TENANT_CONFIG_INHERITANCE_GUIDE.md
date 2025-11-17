# 테넌트별 설정 상속 시스템 가이드

## 개요

테넌트별 설정 상속 시스템은 플랫폼 전역 설정, 테넌트 타입별 기본 설정, 개별 테넌트 설정 간의 상속 구조를 지원하여 모든 테넌트의 유효 설정을 일관성 있게 계산하고 캐시하는 시스템입니다.

## 상속 순서

설정값은 다음 순서로 상속됩니다:

1. **플랫폼 전역 설정** (최하위 우선순위)
2. **테넌트 타입별 기본 설정** (중간 우선순위)
3. **개별 테넌트 설정** (최상위 우선순위)

## 주요 컴포넌트

### 1. 엔티티

- `TenantConfig`: 개별 테넌트의 설정
- `TenantTypeConfig`: 테넌트 타입별 기본 설정
- `PlatformConfig`: 플랫폼 전역 설정 (기존)

### 2. 서비스

- `TenantConfigInheritanceService`: 설정 상속 로직 처리
- `TenantConfigService`: 개별 테넌트 설정 관리
- `TenantConfigCacheService`: 캐시 관리

### 3. 컨트롤러

- `TenantConfigController`: 테넌트 설정 API
- `TenantTypeConfigController`: 테넌트 타입별 설정 API

## API 사용 예시

### 1. 테넌트의 유효 설정 조회

```http
GET /api/v1/tenants/{tenantKey}/configs/effective
```

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "tenantKey": "enterprise-tenant-1",
    "configurations": {
      "max_users": {
        "value": 1000,
        "source": "TENANT_TYPE",
        "description": "Maximum number of users for Enterprise tenants",
        "configType": "NUMBER"
      },
      "max_file_size": {
        "value": 2048,
        "source": "TENANT",
        "description": "Custom file size limit",
        "configType": "NUMBER"
      },
      "support_level": {
        "value": "premium",
        "source": "TENANT_TYPE",
        "description": "Support level for Enterprise tenants",
        "configType": "STRING"
      }
    }
  }
}
```

### 2. 특정 설정 조회

```http
GET /api/v1/tenants/{tenantKey}/configs/effective/{configKey}
```

### 3. 테넌트 개별 설정 관리

```http
# 설정 생성/수정
POST /api/v1/tenants/{tenantKey}/configs
Content-Type: application/json

{
  "configKey": "custom_feature_enabled",
  "configValue": "true",
  "configType": "BOOLEAN",
  "description": "Enable custom feature for this tenant"
}

# 설정 삭제
DELETE /api/v1/tenants/{tenantKey}/configs/{configKey}
```

### 4. 테넌트 타입별 기본 설정 관리

```http
# 타입별 설정 생성/수정
POST /api/v1/tenant-types/{tenantType}/configs
Content-Type: application/json

{
  "configKey": "max_users",
  "configValue": "1000",
  "configType": "NUMBER",
  "description": "Maximum number of users for Enterprise tenants"
}
```

## 시나리오 예시

### 시나리오 1: 플랫폼 설정 상속

**Given**: 플랫폼 전역 설정에 `max_file_size: 100MB`가 설정됨
**When**: 테넌트가 특별한 설정을 하지 않음
**Then**: 테넌트는 자동으로 100MB 제한을 상속받음

### 시나리오 2: 테넌트별 설정 오버라이드

**Given**: 플랫폼 전역 설정이 `max_file_size: 100MB`
**When**: 특정 테넌트가 `max_file_size: 500MB`로 설정
**Then**: 해당 테넌트는 500MB 제한을 사용하고, 다른 테넌트는 100MB 제한 유지

### 시나리오 3: 타입 기본값 적용

**Given**: 전역에 `support_level` 없음
**And**: 타입(ENTERPRISE) 기본 `support_level=premium`
**When**: 테넌트가 미설정
**Then**: `support_level=premium` 적용

### 시나리오 4: 타입 변경 반영

**Given**: 테넌트 타입 INDIVIDUAL → ENTERPRISE로 변경
**When**: 타입 변경 이벤트 발생
**Then**: 캐시 무효화 및 유효 설정에 ENTERPRISE 기본값 반영

## 캐시 관리

시스템은 자동으로 캐시를 관리합니다:

- **설정 변경 시**: 해당 테넌트의 캐시 자동 무효화
- **테넌트 타입 변경 시**: 해당 타입의 모든 테넌트 캐시 무효화
- **플랫폼 설정 변경 시**: 모든 테넌트 캐시 무효화

수동 캐시 무효화:

```http
POST /api/v1/tenants/{tenantKey}/configs/cache/evict
```

## 설정 타입

지원하는 설정 타입:

- `STRING`: 문자열
- `NUMBER`: 숫자 (정수/실수)
- `BOOLEAN`: 불린값
- `JSON`: JSON 객체
- `ENCRYPTED`: 암호화된 값

## 에러 처리

표준화된 에러 코드를 사용합니다:

- `TENANT_CONFIG_NOT_FOUND`: 설정을 찾을 수 없음
- `INVALID_CONFIG_VALUE`: 유효하지 않은 설정값
- `CONFIG_KEY_ALREADY_EXISTS`: 중복된 설정 키
- `CONFIG_INHERITANCE_FAILED`: 상속 처리 실패

## 성능 고려사항

- 설정 조회는 캐시를 통해 최적화됨
- 설정 변경 시에만 캐시 무효화
- 대량 설정 조회 시 배치 처리 권장
- 설정 상속 계산은 비동기로 처리 가능

## 보안 고려사항

- 민감한 설정은 암호화 저장
- 테넌트별 설정 접근 권한 검증
- 감사 로깅으로 설정 변경 이력 추적
- 설정값 유효성 검증

## 모니터링

- 설정 조회 성능 메트릭
- 캐시 히트율 모니터링
- 설정 변경 빈도 추적
- 에러율 및 예외 상황 모니터링



