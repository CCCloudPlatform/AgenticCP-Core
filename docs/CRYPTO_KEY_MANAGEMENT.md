## 암호화 키 관리 가이드 (Key Setup / Rotation / Failure / Cache Policy)

본 문서는 플랫폼 설정의 암호화·복호화 기능 운영을 위한 키 설정, 키 회전, 장애 대응, 캐시 정책을 정리합니다.

### 1) 키 설정 (Configuration)
- **프로퍼티 경로**: `config.cipher.*`
  - `config.cipher.key` (필수): Base64 인코딩된 AES 키(16/24/32 바이트 권장: 32바이트)
  - `config.cipher.secondaryKey` (선택): 키 회전을 위한 보조 키(복호화 fallback 용도)
  - `config.cipher.missingKeyBehavior` (선택): 키 누락 시 동작. `FAIL`(기본) 또는 `READ_ONLY`

- **로컬 예시(`application-local.yml`)**
```yaml
config:
  cipher:
    key: ${CONFIG_CIPHER_KEY:MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDA=}
    # secondaryKey: ${CONFIG_CIPHER_SECONDARY_KEY:}
    # missingKeyBehavior: FAIL  # 또는 READ_ONLY
```

- **실행 시 주입 예시**
```bash
# 환경변수로
export CONFIG_CIPHER_KEY=Base64Encoded32ByteKey==
./start-dev.sh

# JVM 시스템 프로퍼티로
mvn spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-DCONFIG_CIPHER_KEY=Base64Encoded32ByteKey=="
```

- **보안 권고**
  - 운영 키는 비밀관리 솔루션(KMS/Secret Manager, Vault 등)에서 주입.
  - 저장소(깃) 및 이미지에 키를 포함하지 말 것.
  - 로컬 기본 키는 개발 편의용으로만 사용, 외부 반출 금지.

### 2) 키 회전 (Rotation)
- **개념**
  - 주 키(`key`)로 신규 암호화 수행.
  - 회전 기간 동안 보조 키(`secondaryKey`)를 함께 배포하여, 과거 키로 암호화된 데이터 복호화 fallback을 허용.

- **추천 절차**
  1. 새 키 생성(Base64 인코딩) 및 안전 저장.
  2. 애플리케이션에 `secondaryKey`로 기존 키를 배포(읽기만).
  3. 주 키(`key`)를 새 키로 변경. 신규 저장은 새 키로 암호화됨.
  4. 백필 선택(옵션): 중요 항목을 재저장하여 새 키로 재암호화.
  5. 충분한 그레이스 기간 후 `secondaryKey` 제거.

- **현 구현 상태**
  - 설정 항목: `secondaryKey` 필드 및 스켈레톤까지 반영.
  - 복호화 시 보조 키 fallback 로직은 추후 단계에서 확장 가능(이슈 #19 범위 외 확장).

### 3) 장애 대응 (Failure Behavior / Runbook)
- **missingKeyBehavior=FAIL (기본)**
  - 키 누락/잘못된 길이 시 애플리케이션 기동 실패(fail-fast).
  - 생산 환경 권장. 잘못된 설정으로 평문 노출/오작동을 조기에 차단.

- **missingKeyBehavior=READ_ONLY (비권장, 데모/비상용)**
  - 기동은 허용하되 암·복호화 호출 시 예외 발생(READ-ONLY 모드).
  - 운영 전환 시 반드시 `FAIL`로 복귀.

- **운영 Runbook**
  - 기동 실패 시:
    1) 환경변수/시크릿 소스에 `CONFIG_CIPHER_KEY` 주입 여부 확인
    2) Base64 디코딩 길이(16/24/32) 검증
    3) 잘못된 값이면 교체 후 재배포
  - 복호화 실패(6017)/암호문 포맷 오류(6019) 발생 시:
    1) 해당 설정의 저장 경로 점검(이중 암호화, 손상 여부)
    2) 필요 시 백업에서 복구 또는 정상 평문으로 재저장(서비스가 저장 시 암호화)

### 4) 캐시 정책 (Cache Policy)
- **기본 원칙**
  - ENCRYPTED 설정은 기본적으로 마스킹(`***`)되어 반환 → 캐시 정책과 무관.
  - `showSecret=true`로 평문 반환 시, 중간자/클라이언트 캐시 금지.

- **헤더**
  - `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`
  - `Pragma: no-cache`

- **권한 및 로깅**
  - `showSecret=true`는 `ROLE_ADMIN` 또는 `ROLE_PLATFORM_ADMIN` 필요.
  - 감사 로그: `actor`, `reason`, `X-Forwarded-For` 등 기록.

### 5) 개발 참고
- 서비스 레이어는 ENCRYPTED 타입 저장 시 자동 암호화, 기본 조회 시 마스킹.
- `showSecret=true` + 권한 통과 시 복호화 후 응답, 실패는 도메인 오류코드 매핑:
  - `PLATFORM_6017` 복호화 실패 (DECRYPTION_FAILED)
  - `PLATFORM_6019` 암호문 형식 오류 (ENCRYPTED_PAYLOAD_INVALID)

### 6) 점검 체크리스트
- [ ] `config.cipher.key`가 환경별로 안전하게 주입되는가?
- [ ] 키 길이(16/24/32)와 Base64 인코딩이 유효한가?
- [ ] 회전 시 `secondaryKey`와 배포 순서가 올바른가?
- [ ] `showSecret=true` 경로에 권한 검사/감사 로깅/캐시 금지가 적용되는가?
- [ ] 로그에 평문/키/암호문이 출력되지 않는가?


