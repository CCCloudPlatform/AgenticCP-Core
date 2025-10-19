# AgenticCP 감사 로깅 가이드

## 📋 목차

1. [감사 로깅의 역할](#감사-로깅의-역할)
2. [클래스 레벨 감사 로깅](#클래스-레벨-감사-로깅)
3. [메서드 레벨 감사 로깅](#메서드-레벨-감사-로깅)
4. [클래스와 메서드 레벨 조합 사용](#클래스와-메서드-레벨-조합-사용)
5. [감사 로깅 저장 형식](#감사-로깅-저장-형식)
6. [애플리케이션 로깅 vs 감사 로깅](#애플리케이션-로깅-vs-감사-로깅)
7. [데이터 마스킹 가이드 (@Masked)](#-데이터-마스킹-masked)
8. [실제 사용 예시](#실제-사용-예시)
9. [요약](#요약)

---

## 🎯 감사 로깅의 역할

### 1. 감사 로깅이란?

감사 로깅은 시스템의 중요한 작업과 이벤트를 기록하여 **보안, 컴플라이언스, 디버깅** 목적으로 사용되는 로깅 시스템입니다.

### 2. 주요 목적

- **보안 감사**: 사용자 행위 추적 및 보안 사고 대응
- **컴플라이언스**: 규정 준수를 위한 감사 증적 생성
- **운영 모니터링**: 시스템 사용 현황 및 성능 분석
- **문제 해결**: 장애 발생 시 원인 분석 및 복구

### 3. 감사 로깅 구조

```mermaid
graph TD
    A[Controller Method] --> B{감사 로깅 적용}
    B --> C[@AuditController 클래스 레벨]
    B --> D[@AuditRequired 메서드 레벨]
    C --> E[AuditAspect]
    D --> E
    E --> F[AuditService]
    F --> G[AuditLogger]
    G --> H[JSON 로그 출력]
```

---

## 🏢 클래스 레벨 감사 로깅

### 1. @AuditController 애노테이션

클래스에 적용하여 **모든 메서드**에 자동으로 감사 로깅을 적용합니다.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditController {
    AuditResourceType resourceType();              // 필수: 리소스 타입
    AuditSeverity defaultSeverity() default AuditSeverity.MEDIUM;  // 심각도
    boolean defaultIncludeRequestData() default false;             // 요청 데이터 포함 여부
    boolean defaultIncludeResponseData() default false;            // 응답 데이터 포함 여부
    String[] targetHttpMethods() default {"POST", "PUT", "PATCH", "DELETE"};  // 대상 HTTP 메서드
    String[] excludeMethods() default {};          // 제외할 메서드명
}
```

### 2. 언제 사용해야 하는가?

**✅ 클래스 레벨 감사 로깅을 사용해야 하는 경우:**

- **CRUD 컨트롤러**: 대부분의 메서드가 동일한 리소스 타입을 다룰 때
- **관리자 기능**: 모든 작업이 중요하고 감사가 필요할 때
- **보안 민감한 컨트롤러**: 모든 접근을 기록해야 할 때
- **일관된 로깅**: 클래스 내 모든 메서드에 동일한 감사 정책 적용할 때

**❌ 클래스 레벨 감사 로깅을 사용하지 말아야 하는 경우:**

- **혼재된 리소스**: 서로 다른 리소스 타입을 다루는 메서드가 섞여 있을 때
- **선택적 감사**: 일부 메서드만 감사가 필요할 때
- **성능 민감**: 모든 메서드에 로깅이 성능에 영향을 줄 때

### 3. 사용 예시

```java
// ✅ 좋은 예 - 사용자 관리 컨트롤러
@RestController
@RequestMapping("/api/v1/users")
@AuditController(
    resourceType = AuditResourceType.USER,
    defaultSeverity = AuditSeverity.HIGH,
    defaultIncludeRequestData = true,
    targetHttpMethods = {"POST", "PUT", "PATCH", "DELETE"},
    excludeMethods = {"healthCheck"}
)
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        // 자동으로 감사 로깅 적용됨
        // - action: "createUser"
        // - resourceType: USER
        // - severity: HIGH
        // - requestData 포함
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UserUpdateRequest request) {
        // 자동으로 감사 로깅 적용됨
        // - action: "updateUser"
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        // excludeMethods에 포함되어 감사 로깅 제외됨
        return ResponseEntity.ok("OK");
    }
}

// ✅ 좋은 예 - 보안 컨트롤러
@RestController
@RequestMapping("/api/v1/security")
@AuditController(
    resourceType = AuditResourceType.SECURITY,
    defaultSeverity = AuditSeverity.CRITICAL,
    defaultIncludeRequestData = true,
    defaultIncludeResponseData = true,
    targetHttpMethods = {"GET", "POST", "PUT", "DELETE"}  // 모든 HTTP 메서드 감사
)
public class SecurityController {
    
    @GetMapping("/policies")
    public ResponseEntity<List<SecurityPolicy>> getPolicies() {
        // 모든 GET 요청도 감사 로깅됨
    }
    
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        // 로그인 시도 감사 로깅됨
    }
}
```

---

## 🔧 메서드 레벨 감사 로깅

### 1. @AuditRequired 애노테이션

개별 메서드에 적용하여 **세밀한 감사 로깅**을 구현합니다.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditRequired {
    String action();                              // 필수: 액션명
    AuditResourceType resourceType();             // 필수: 리소스 타입
    String description() default "";              // 설명
    boolean includeRequestData() default false;   // 요청 데이터 포함 여부
    boolean includeResponseData() default false;  // 응답 데이터 포함 여부
    AuditSeverity severity() default AuditSeverity.INFO;  // 심각도
}
```

### 2. 언제 사용해야 하는가?

**✅ 메서드 레벨 감사 로깅을 사용해야 하는 경우:**

- **특정 메서드만 감사**: 클래스 내 일부 메서드만 중요할 때
- **세밀한 제어**: 메서드별로 다른 감사 정책이 필요할 때
- **혼재된 리소스**: 클래스가 여러 리소스 타입을 다룰 때
- **선택적 데이터 포함**: 특정 메서드에서만 요청/응답 데이터가 필요할 때

**❌ 메서드 레벨 감사 로깅을 사용하지 말아야 하는 경우:**

- **일괄 적용**: 클래스 내 모든 메서드에 동일한 정책 적용할 때
- **보수적 접근**: 모든 메서드를 기본적으로 감사하고 싶을 때

### 3. 사용 예시

```java
// ✅ 좋은 예 - 혼재된 리소스 타입 컨트롤러
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    @PostMapping("/users")
    @AuditRequired(
        action = "createUser",
        resourceType = AuditResourceType.USER,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "관리자가 새 사용자를 생성합니다"
    )
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        // 사용자 생성만 감사 로깅됨
    }
    
    @PostMapping("/tenants")
    @AuditRequired(
        action = "createTenant",
        resourceType = AuditResourceType.TENANT,
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = true,
        description = "새 테넌트를 생성합니다"
    )
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        // 테넌트 생성만 감사 로깅됨 (더 높은 심각도)
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        // 감사 로깅 없음 (조회 작업)
    }
}

// ✅ 좋은 예 - 특정 작업만 감사
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    @PostMapping("/process")
    @AuditRequired(
        action = "processPayment",
        resourceType = AuditResourceType.PAYMENT,
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = true,
        description = "결제 처리 작업"
    )
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        // 결제 처리만 감사 로깅됨
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistory>> getPaymentHistory() {
        // 감사 로깅 없음 (조회 작업)
    }
    
    @PostMapping("/refund")
    @AuditRequired(
        action = "refundPayment",
        resourceType = AuditResourceType.PAYMENT,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "결제 환불 작업"
    )
    public ResponseEntity<RefundResponse> refundPayment(@Valid @RequestBody RefundRequest request) {
        // 환불 처리만 감사 로깅됨
    }
}
```

---

## 🔄 클래스와 메서드 레벨 조합 사용

### 1. 우선순위 규칙

**메서드 레벨 `@AuditRequired`가 클래스 레벨 `@AuditController`보다 우선합니다.**

```java
@AuditController(
    resourceType = AuditResourceType.USER,
    defaultSeverity = AuditSeverity.MEDIUM
)
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        // 클래스 레벨 설정 적용:
        // - resourceType: USER
        // - severity: MEDIUM
    }
    
    @PostMapping("/admin")
    @AuditRequired(
        action = "createUserByAdmin",
        resourceType = AuditResourceType.ADMIN,
        severity = AuditSeverity.HIGH,
        includeRequestData = true
    )
    public ResponseEntity<UserResponse> createUserByAdmin(@RequestBody UserCreateRequest request) {
        // 메서드 레벨 설정 적용 (우선순위):
        // - action: "createUserByAdmin" (명시적 지정)
        // - resourceType: ADMIN (클래스 설정 덮어씀)
        // - severity: HIGH (클래스 설정 덮어씀)
        // - includeRequestData: true (메서드에서 설정)
    }
}
```

### 2. 조합 사용 시나리오

```java
// ✅ 좋은 예 - 기본 + 특별 처리
@RestController
@RequestMapping("/api/v1/security")
@AuditController(
    resourceType = AuditResourceType.SECURITY,
    defaultSeverity = AuditSeverity.MEDIUM,
    targetHttpMethods = {"POST", "PUT", "DELETE"}
)
public class SecurityController {
    
    // 클래스 레벨 설정 적용
    @PostMapping("/policy")
    public ResponseEntity<SecurityPolicy> createPolicy(@RequestBody PolicyRequest request) {
        // - resourceType: SECURITY
        // - severity: MEDIUM
        // - action: "createPolicy" (자동 생성)
    }
    
    // 메서드 레벨 설정으로 덮어씀
    @PostMapping("/critical-action")
    @AuditRequired(
        action = "performCriticalSecurityAction",
        resourceType = AuditResourceType.SECURITY,
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = true,
        description = "중요한 보안 작업 수행"
    )
    public ResponseEntity<CriticalActionResponse> performCriticalAction(@RequestBody CriticalRequest request) {
        // - action: "performCriticalSecurityAction" (명시적 지정)
        // - resourceType: SECURITY (동일)
        // - severity: CRITICAL (클래스 설정 덮어씀)
        // - includeRequestData: true (추가 설정)
        // - includeResponseData: true (추가 설정)
    }
    
    // GET 메서드는 클래스 설정에서 제외됨
    @GetMapping("/status")
    public ResponseEntity<SecurityStatus> getStatus() {
        // 감사 로깅 없음 (targetHttpMethods에 GET 없음)
    }
}
```

### 3. 실제 로깅 결과 비교

```java
// 클래스 레벨만 사용한 경우
@AuditController(resourceType = AuditResourceType.USER)
public class UserController {
    public UserResponse createUser(UserCreateRequest request) {
        // 로깅 결과:
        // {
        //   "action": "createUser",
        //   "resourceType": "USER",
        //   "severity": "MEDIUM",
        //   "includeRequestData": false,
        //   "includeResponseData": false
        // }
    }
}

// 메서드 레벨로 덮어쓴 경우
@AuditController(resourceType = AuditResourceType.USER)
public class UserController {
    @AuditRequired(
        action = "createUserByAdmin",
        resourceType = AuditResourceType.ADMIN,
        severity = AuditSeverity.HIGH,
        includeRequestData = true
    )
    public UserResponse createUserByAdmin(UserCreateRequest request) {
        // 로깅 결과:
        // {
        //   "action": "createUserByAdmin",        // 메서드 레벨 설정
        //   "resourceType": "ADMIN",             // 메서드 레벨 설정
        //   "severity": "HIGH",                  // 메서드 레벨 설정
        //   "includeRequestData": true,          // 메서드 레벨 설정
        //   "includeResponseData": false         // 클래스 레벨 기본값
        // }
    }
}
```

---

## 💾 감사 로깅 저장 형식

### 1. AuditEventDto 구조

```java
public record AuditEventDto(
    String action,                    // 액션명 (예: "createUser", "deletePost")
    AuditResourceType resourceType,   // 리소스 타입 (USER, TENANT, SECURITY 등)
    String httpMethod,               // HTTP 메서드 (GET, POST, PUT, DELETE)
    String requestPath,              // 요청 경로 (예: "/api/v1/users")
    String operationSummary,         // 작업 요약
    String controllerName,           // 컨트롤러 클래스명
    String methodName,               // 메서드명
    AuditSeverity severity,          // 심각도 (CRITICAL, HIGH, MEDIUM, LOW, INFO)
    Instant timestamp,               // 타임스탬프 (KST)
    String requestId,                // 요청 ID (추적용)
    String tenantId,                 // 테넌트 ID
    String userId,                   // 사용자 ID
    String clientIp,                 // 클라이언트 IP
    boolean success,                 // 성공 여부
    String error,                    // 에러 메시지 (실패 시)
    Map<String, Object> requestData, // 요청 데이터 (includeRequestData=true 시)
    Map<String, Object> responseData,// 응답 데이터 (includeResponseData=true 시)
    Map<String, Object> metadata     // 추가 메타데이터
)
```

### 2. JSON 로그 출력 예시

```json

{
  "action": "createUser",
  "resourceType": "USER",
  "httpMethod": "POST",
  "requestPath": "/api/v1/users",
  "operationSummary": "사용자 생성",
  "controllerName": "UserController",
  "methodName": "createUser",
  "severity": "HIGH",
  "timestamp": "2024-01-15T10:30:00.123Z",
  "requestId": "req-12345678-1234-1234-1234-123456789abc",
  "tenantId": "tenant-001",
  "userId": "user-12345",
  "clientIp": "192.168.1.100",
  "success": true,
  "error": null,
  "requestData": {
    "username": "john_doe",
    "email": "john@example.com",
    "name": "John Doe"
  },
  "responseData": {
    "id": 12345,
    "username": "john_doe",
    "email": "john@example.com",
    "status": "ACTIVE"
  },
  "metadata": {}
}

// 실패한 로그인 시도
{
  "action": "login",
  "resourceType": "SECURITY",
  "httpMethod": "POST",
  "requestPath": "/api/v1/auth/login",
  "operationSummary": "사용자 로그인",
  "controllerName": "AuthController",
  "methodName": "login",
  "severity": "CRITICAL",
  "timestamp": "2024-01-15T10:35:00.456Z",
  "requestId": "req-87654321-4321-4321-4321-cba987654321",
  "tenantId": "tenant-001",
  "userId": null,
  "clientIp": "192.168.1.200",
  "success": false,
  "error": "Invalid credentials",
  "requestData": {
    "username": "hacker",
    "password": "***"  // 실제로는 마스킹됨
  },
  "responseData": null,
  "metadata": {
    "attemptCount": 3,
    "blocked": true
  }
}
```

### 3. 로그 레벨별 출력

```java
// AuditLogger에서 심각도에 따라 다른 로그 레벨 사용
private static final Map<AuditSeverity, BiConsumer<Logger, String>> logActions
        = new EnumMap<>(AuditSeverity.class);

static {
    logActions.put(AuditSeverity.CRITICAL, Logger::error);  // ERROR 레벨
    logActions.put(AuditSeverity.HIGH, Logger::warn);       // WARN 레벨
    logActions.put(AuditSeverity.MEDIUM, Logger::info);     // INFO 레벨
    logActions.put(AuditSeverity.LOW, Logger::info);        // INFO 레벨
    logActions.put(AuditSeverity.INFO, Logger::info);       // INFO 레벨
}
```

---

## 🔄 애플리케이션 로깅 vs 감사 로깅

### 1. 두 로깅의 차이점

| 구분 | 감사 로깅 (Audit Logging) | 애플리케이션 로깅 (Application Logging) |
|------|---------------------------|----------------------------------------|
| **목적** | 보안, 컴플라이언스, 감사 증적 생성 | 개발, 디버깅, 운영 모니터링 |
| **대상** | 중요한 비즈니스 작업 (생성, 수정, 삭제, 로그인) | 모든 작업 (조회, 생성, 수정, 삭제, 에러) |
| **형식** | 구조화된 JSON 로그 | 일반적인 텍스트 로그 |
| **저장 위치** | 별도의 감사 로그 파일 (`audit.log`) | 애플리케이션 로그 파일 (`application.log`) |
| **사용자** | 보안팀, 감사팀, 컴플라이언스 담당자 | 개발자, 운영팀, DevOps |
| **자동화** | `@AuditController`, `@AuditRequired`로 자동 생성 | `log.info()`, `log.error()` 등으로 수동 작성 |
| **데이터 포함** | 요청/응답 데이터, 사용자 정보, IP 등 | 개발자가 필요한 정보만 선택적 포함 |

### 2. 언제 애플리케이션 로깅을 사용하는가?

**✅ 애플리케이션 로깅을 사용해야 하는 경우:**

- **디버깅**: 코드 흐름 추적 및 문제 원인 파악
- **성능 모니터링**: 처리 시간 및 성능 지표 측정
- **운영 모니터링**: 시스템 상태 및 사용량 추적
- **에러 추적**: 예외 발생 시 상세한 컨텍스트 정보
- **비즈니스 로직 추적**: 복잡한 비즈니스 프로세스의 중간 단계

**❌ 애플리케이션 로깅을 사용하지 않아도 되는 경우:**

- **단순한 CRUD 작업**: 감사 로깅으로 충분한 경우
- **성능이 중요한 경우**: 로깅 오버헤드를 최소화해야 할 때
- **보안이 최우선**: 민감한 정보 노출 위험이 있는 경우

### 3. 애플리케이션 로깅 사용 예시

```java
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@AuditController(
    resourceType = AuditResourceType.USER,
    defaultSeverity = AuditSeverity.MEDIUM
)
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        // 1. 요청 시작 로깅 (디버깅용)
        log.info("사용자 생성 요청 시작: username={}, email={}", 
                request.getUsername(), request.getEmail());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 2. 비즈니스 로직 실행
            UserResponse user = userService.createUser(request);
            
            // 3. 성공 로깅 (성능 모니터링용)
            long duration = System.currentTimeMillis() - startTime;
            log.info("사용자 생성 성공: userId={}, username={}, 처리시간={}ms", 
                    user.getId(), user.getUsername(), duration);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
            
        } catch (DuplicateUserException e) {
            // 4. 비즈니스 예외 로깅 (문제 추적용)
            log.warn("사용자 생성 실패 - 중복 사용자: username={}, error={}", 
                    request.getUsername(), e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // 5. 시스템 예외 로깅 (에러 추적용)
            log.error("사용자 생성 중 예상치 못한 오류: username={}", 
                    request.getUsername(), e);
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        // 조회 작업은 감사 로깅 없음, 애플리케이션 로깅만 사용
        log.debug("사용자 조회 요청: userId={}", id);
        
        UserResponse user = userService.getUserById(id);
        
        log.debug("사용자 조회 완료: userId={}, username={}", 
                user.getId(), user.getUsername());
        
        return ResponseEntity.ok(user);
    }
}
```

### 4. 로그 레벨별 사용 가이드

```java
@Service
@Slf4j
public class UserService {
    
    public UserResponse createUser(UserCreateRequest request) {
        // ERROR: 시스템 오류, 복구 불가능한 문제
        log.error("데이터베이스 연결 실패: {}", e.getMessage());
        
        // WARN: 비즈니스 예외, 예상 가능한 문제
        log.warn("중복된 사용자명: username={}", request.getUsername());
        
        // INFO: 중요한 비즈니스 이벤트, 성능 모니터링
        log.info("사용자 생성 시작: username={}", request.getUsername());
        log.info("사용자 생성 완료: userId={}, 처리시간={}ms", userId, duration);
        
        // DEBUG: 개발/디버깅용 상세 정보
        log.debug("사용자 중복 검사 시작: username={}", request.getUsername());
        log.debug("사용자 중복 검사 완료: exists={}", exists);
        
        // TRACE: 매우 상세한 실행 흐름 (성능에 영향)
        log.trace("메서드 진입: createUser(request={})", request);
    }
}
```

#### 로그 파일 구조

```
logs/
├── application.log              # 애플리케이션 로그 (현재)
├── application.2024-01-14.0.log # 애플리케이션 로그 (과거)
├── application.2024-01-14.1.log
├── audit.log                   # 감사 로그 (현재)
├── audit.2024-01-14.0.log      # 감사 로그 (과거)
└── audit.2024-01-14.1.log
```

### 5. 실제 로그 비교 예시

#### 동일한 작업에 대한 두 로그 비교

**애플리케이션 로그 (application.log):**
```
2024-01-15 10:30:00.123 INFO  [http-nio-8080-exec-1] c.a.c.controller.UserController : 사용자 생성 요청 시작: username=john_doe, email=john@example.com
2024-01-15 10:30:00.145 INFO  [http-nio-8080-exec-1] c.a.c.service.UserService : 사용자 생성 시작: username=john_doe
2024-01-15 10:30:00.156 DEBUG [http-nio-8080-exec-1] c.a.c.service.UserService : 사용자 중복 검사 시작: username=john_doe
2024-01-15 10:30:00.167 DEBUG [http-nio-8080-exec-1] c.a.c.service.UserService : 사용자 중복 검사 완료: exists=false
2024-01-15 10:30:00.178 INFO  [http-nio-8080-exec-1] c.a.c.repository.UserRepository : 사용자 저장 시작: username=john_doe
2024-01-15 10:30:00.189 INFO  [http-nio-8080-exec-1] c.a.c.repository.UserRepository : 사용자 저장 완료: userId=12345
2024-01-15 10:30:00.200 INFO  [http-nio-8080-exec-1] c.a.c.service.EmailService : 환영 이메일 발송 시작: userId=12345
2024-01-15 10:30:00.345 INFO  [http-nio-8080-exec-1] c.a.c.service.EmailService : 환영 이메일 발송 완료: userId=12345
2024-01-15 10:30:00.346 INFO  [http-nio-8080-exec-1] c.a.c.service.UserService : 사용자 생성 완료: userId=12345, 처리시간=201ms
2024-01-15 10:30:00.347 INFO  [http-nio-8080-exec-1] c.a.c.controller.UserController : 사용자 생성 성공: userId=12345, username=john_doe, 처리시간=224ms
```

**감사 로그 (audit.log):**
```json
{
  "action": "createUser",
  "resourceType": "USER",
  "httpMethod": "POST",
  "requestPath": "/api/v1/users",
  "operationSummary": "사용자 생성",
  "controllerName": "UserController",
  "methodName": "createUser",
  "severity": "MEDIUM",
  "timestamp": "2024-01-15T10:30:00.123Z",
  "requestId": "req-12345678-1234-1234-1234-123456789abc",
  "tenantId": "tenant-001",
  "userId": "admin-001",
  "clientIp": "192.168.1.100",
  "success": true,
  "error": null,
  "requestData": {
    "username": "john_doe",
    "email": "john@example.com",
    "name": "John Doe"
  },
  "responseData": null,
  "metadata": {}
}
```

### 6. 사용 시나리오별 로깅 전략

#### 시나리오 1: 보안 사고 조사
```
🔍 보안팀: "어제 밤 11시경에 admin 계정이 삭제되었는데, 누가 했는지 추적해줘"

📋 감사 로그에서 확인:
- 2024-01-14 23:15:00 - action: "deleteUser", userId: "admin-001", clientIp: "192.168.1.100"

📋 애플리케이션 로그에서 확인:
- 2024-01-14 23:14:58 - "사용자 삭제 요청: userId=12345"
- 2024-01-14 23:14:59 - "관련 데이터 정리 시작: userId=12345"
- 2024-01-14 23:15:00 - "사용자 삭제 완료: userId=12345"
```

#### 시나리오 2: 성능 문제 해결
```
🐛 개발팀: "사용자 생성이 느린데 원인이 뭐야?"

📋 애플리케이션 로그에서 확인:
- 10:30:00.123 - "사용자 생성 요청: username=john_doe"
- 10:30:00.145 - "사용자 생성 시작: username=john_doe"
- 10:30:00.567 - "이메일 전송 완료: userId=12345"  ← 422ms 소요!
- 10:30:00.568 - "사용자 생성 완료: userId=12345"

💡 문제 발견: 이메일 전송이 422ms나 걸림
```

#### 시나리오 3: 비즈니스 로직 디버깅
```
🐛 개발팀: "사용자 생성 시 중복 검사가 제대로 안 되는 것 같아"

📋 애플리케이션 로그에서 확인:
- 10:30:00.156 - "사용자 중복 검사 시작: username=john_doe"
- 10:30:00.167 - "사용자 중복 검사 완료: exists=false"
- 10:30:00.178 - "사용자 저장 시작: username=john_doe"
- 10:30:00.189 - "사용자 저장 완료: userId=12345"

💡 중복 검사는 정상 작동, 다른 원인 추적 필요
```

### 7. 로깅 전략 요약

| 상황 | 감사 로깅 | 애플리케이션 로깅 | 이유 |
|------|-----------|------------------|------|
| **CRUD 작업** | ✅ 필수 | ✅ 권장 | 보안 + 개발 편의성 |
| **조회 작업** | ❌ 불필요 | ✅ 권장 | 개발/운영 모니터링 |
| **로그인/로그아웃** | ✅ 필수 | ✅ 권장 | 보안 + 문제 추적 |
| **에러 발생** | ✅ 필수 | ✅ 필수 | 보안 + 디버깅 |
| **성능 모니터링** | ❌ 불필요 | ✅ 필수 | 성능 분석 |
| **개발 중** | ✅ 권장 | ✅ 필수 | 학습 + 디버깅 |

**결론**: 감사 로깅은 보안과 컴플라이언스를 위해 필수이고, 애플리케이션 로깅은 개발과 운영을 위해 권장됩니다.

---

## 🔒 데이터 마스킹 가이드 (@Masked)

### 1) 사용법 (핵심)

민감 필드에 `@Masked`를 붙이면 감사/로그 경로에서 자동 마스킹됩니다. 필요 시 `type`으로 룰 지정.

```java
public class UserCreateRequest {
    @Masked // 기본 마스킹
    private String name;

    @Masked(type = MaskingType.EMAIL) // 이메일 전용 마스킹
    private String email;
}
```

### 2) 새로운 마스킹 룰 추가하고 싶을 시

- 위치: `src/main/java/com/agenticcp/core/common/logging/masking/strategy/`
- 절차: (1) 새 전략 클래스 추가(`mask(String)` 구현) → (2) `MaskingType`에 enum 추가 → (3) `MaskingService` 전략 매핑 등록
- 사용: 필드에 `@Masked(type = MaskingType.MY_NEW_TYPE)` 지정

### 3) 현재 제공 마스킹 룰

- DEFAULT: 앞/뒤 일부 노출, 중앙 `*` 치환
- EMAIL: 로컬/도메인 일부만 노출
- PASSWORD/SECRET_KEY/TOKEN: 전면 마스킹 또는 극소 노출
- PHONE_NUMBER: 끝자리만 일부 노출
- CREDIT_CARD: BIN+끝 4자리 노출
- IP_ADDRESS: IPv4 마지막/IPv6 후미 세그먼트 마스킹

참고 파일: `common/logging/masking/Masked.java`, `.../strategy/*.java`, `MaskingService`, `LogMaskingAspect`, `LogMaskingUtils`.

---

## 🎯 실제 사용 예시

### 1. 완전한 사용자 관리 컨트롤러

```java
@RestController
@RequestMapping("/api/v1/users")
@Validated
@Slf4j
@AuditController(
    resourceType = AuditResourceType.USER,
    defaultSeverity = AuditSeverity.MEDIUM,
    defaultIncludeRequestData = true,
    targetHttpMethods = {"POST", "PUT", "PATCH", "DELETE"},
    excludeMethods = {"getCurrentUser", "updateProfile"}
)
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    // 클래스 레벨 감사 로깅 적용
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("사용자 생성 요청: username={}", request.getUsername());
        
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    // 클래스 레벨 감사 로깅 적용
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("사용자 수정 요청: userId={}", id);
        
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
    
    // 클래스 레벨 감사 로깅 적용
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive Long id) {
        log.info("사용자 삭제 요청: userId={}", id);
        
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    // 메서드 레벨 감사 로깅으로 덮어씀
    @PatchMapping("/{id}/status")
    @AuditRequired(
        action = "changeUserStatus",
        resourceType = AuditResourceType.USER,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true,
        description = "사용자 상태 변경 (활성화/비활성화)"
    )
    public ResponseEntity<UserResponse> changeUserStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UserStatusChangeRequest request) {
        
        log.info("사용자 상태 변경 요청: userId={}, status={}", id, request.getStatus());
        
        UserResponse user = userService.changeUserStatus(id, request.getStatus());
        return ResponseEntity.ok(user);
    }
    
    // 클래스 레벨에서 제외됨 (excludeMethods)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserResponse user = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(user);
    }
    
    // 클래스 레벨에서 제외됨 (excludeMethods)
    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        
        UserResponse user = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(user);
    }
    
    // GET 메서드는 클래스 레벨에서 제외됨
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable @Positive Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    // GET 메서드는 클래스 레벨에서 제외됨
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        Page<UserResponse> users = userService.getUsers(page, size, status);
        return ResponseEntity.ok(users);
    }
}
```

### 2. 보안 컨트롤러 예시

```java
@RestController
@RequestMapping("/api/v1/security")
@Validated
@Slf4j
public class SecurityController {
    
    private final SecurityService securityService;
    
    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }
    
    // 메서드 레벨 감사 로깅만 사용
    @PostMapping("/login")
    @AuditRequired(
        action = "userLogin",
        resourceType = AuditResourceType.SECURITY,
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        description = "사용자 로그인 시도"
    )
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 시도: username={}", request.getUsername());
        
        TokenResponse token = securityService.authenticate(request);
        return ResponseEntity.ok(token);
    }
    
    @PostMapping("/logout")
    @AuditRequired(
        action = "userLogout",
        resourceType = AuditResourceType.SECURITY,
        severity = AuditSeverity.MEDIUM,
        description = "사용자 로그아웃"
    )
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        securityService.logout(token);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/change-password")
    @AuditRequired(
        action = "changePassword",
        resourceType = AuditResourceType.SECURITY,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "비밀번호 변경"
    )
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        securityService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
    
    // 조회 작업은 감사 로깅 없음
    @GetMapping("/profile")
    public ResponseEntity<SecurityProfile> getSecurityProfile(Authentication authentication) {
        SecurityProfile profile = securityService.getSecurityProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }
}
```

### 3. 생성된 감사 로그 예시

```json
// 로그인 시도 감사 로그
{
  "action": "userLogin",
  "resourceType": "SECURITY",
  "httpMethod": "POST",
  "requestPath": "/api/v1/security/login",
  "operationSummary": "사용자 로그인",
  "controllerName": "SecurityController",
  "methodName": "login",
  "severity": "CRITICAL",
  "timestamp": "2024-01-15T10:40:00.789Z",
  "requestId": "req-11111111-2222-3333-4444-555555555555",
  "tenantId": "tenant-001",
  "userId": "user-12345",
  "clientIp": "192.168.1.200",
  "success": true,
  "error": null,
  "requestData": {
    "username": "john_doe",
    "password": "***"  // 실제로는 마스킹됨
  },
  "responseData": null,
  "metadata": {}
}
```

---

## 📋 요약

### 1. 감사 로깅 선택 가이드

| 상황 | 추천 방법 | 이유 |
|------|-----------|------|
| CRUD 컨트롤러 | 클래스 레벨 `@AuditController` | 모든 메서드가 동일한 리소스 타입 |
| 보안 컨트롤러 | 클래스 레벨 `@AuditController` | 모든 작업이 중요하고 감사 필요 |
| 혼재된 리소스 | 메서드 레벨 `@AuditRequired` | 리소스 타입이 다양함 |
| 선택적 감사 | 메서드 레벨 `@AuditRequired` | 일부 메서드만 감사 필요 |
| 세밀한 제어 | 메서드 레벨 `@AuditRequired` | 메서드별로 다른 감사 정책 |

### 2. 주요 원칙

- **메서드 레벨이 클래스 레벨보다 우선**
- **보안 관련 작업은 높은 심각도 사용**
- **민감한 데이터는 마스킹 처리**
- **성능을 고려하여 필요한 경우에만 데이터 포함**
- **일관된 액션명과 리소스 타입 사용**

### 3. 체크리스트

**감사 로깅 적용 전:**
- [ ] 어떤 작업을 감사할지 명확히 정의
- [ ] 적절한 리소스 타입 선택
- [ ] 심각도 레벨 결정
- [ ] 요청/응답 데이터 포함 여부 결정

**감사 로깅 적용 후:**
- [ ] 로그 출력 확인
- [ ] 보안 정책 준수 확인

이 가이드를 따라 일관되고 효과적인 감사 로깅을 구현하시기 바랍니다.
