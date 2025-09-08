# AgenticCP API 설계 가이드라인

## 📋 목차

1. [REST API 설계 원칙](#rest-api-설계-원칙)
2. [URL 설계 규칙](#url-설계-규칙)
3. [HTTP 메서드 사용](#http-메서드-사용)
4. [상태 코드 사용](#상태-코드-사용)
5. [요청/응답 형식](#요청응답-형식)
6. [에러 처리](#에러-처리)
7. [버전 관리](#버전-관리)
8. [보안 고려사항](#보안-고려사항)

---

## 🎯 REST API 설계 원칙

### 1. RESTful 설계 원칙

- **리소스 중심**: URL은 리소스를 나타내야 함
- **HTTP 메서드 활용**: GET, POST, PUT, PATCH, DELETE 적절히 사용
- **상태 코드 활용**: 적절한 HTTP 상태 코드 반환
- **일관성**: 전체 API에서 일관된 패턴 유지

### 2. 설계 원칙

```java
// ✅ 좋은 예 - 리소스 중심
GET    /api/v1/users              # 사용자 목록
GET    /api/v1/users/{id}         # 특정 사용자
POST   /api/v1/users              # 사용자 생성
PUT    /api/v1/users/{id}         # 사용자 전체 수정
PATCH  /api/v1/users/{id}         # 사용자 부분 수정
DELETE /api/v1/users/{id}         # 사용자 삭제

// ❌ 나쁜 예 - 액션 중심
GET    /api/v1/getUsers
POST   /api/v1/createUser
POST   /api/v1/updateUser
POST   /api/v1/deleteUser
```

---

## 🔗 URL 설계 규칙

### 1. URL 구조

```
https://api.agenticcp.com/api/v1/users/123/profiles/456
│                    │    │   │     │   │        │
│                    │    │   │     │   │        └─ 하위 리소스 ID
│                    │    │   │     │   └────────── 하위 리소스
│                    │    │   │     └────────────── 리소스 ID
│                    │    │   └──────────────────── 리소스명
│                    │    └──────────────────────── API 버전
│                    └───────────────────────────── API 경로
└────────────────────────────────────────────────── 도메인
```

### 2. 네이밍 규칙

```java
// ✅ 좋은 예
/api/v1/users                    # 복수형 사용
/api/v1/users/{id}               # 리소스 ID
/api/v1/users/{id}/profiles      # 하위 리소스
/api/v1/users/{id}/profiles/{profileId}  # 하위 리소스 ID

// ❌ 나쁜 예
/api/v1/user                     # 단수형
/api/v1/users/{userId}           # 불필요한 접두사
/api/v1/getUsers                 # 동사 사용
```

### 3. 쿼리 파라미터

```java
// ✅ 좋은 예
GET /api/v1/users?page=0&size=20&sort=createdAt,desc
GET /api/v1/users?status=ACTIVE&name=김
GET /api/v1/users?createdAfter=2024-01-01&createdBefore=2024-12-31

// ❌ 나쁜 예
GET /api/v1/users?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc
GET /api/v1/users?userStatus=ACTIVE&userName=김
```

---

## 📡 HTTP 메서드 사용

### 1. GET - 조회

```java
@GetMapping("/users")
public ResponseEntity<Page<UserResponse>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status) {
    
    Page<UserResponse> users = userService.getUsers(page, size, status);
    return ResponseEntity.ok(users);
}

@GetMapping("/users/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(user);
}
```

### 2. POST - 생성

```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody UserCreateRequest request) {
    
    UserResponse user = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
}

// 액션 기반 POST
@PostMapping("/users/{id}/activate")
public ResponseEntity<Void> activateUser(@PathVariable Long id) {
    userService.activateUser(id);
    return ResponseEntity.ok().build();
}
```

### 3. PUT - 전체 수정

```java
@PutMapping("/users/{id}")
public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request) {
    
    UserResponse user = userService.updateUser(id, request);
    return ResponseEntity.ok(user);
}
```

### 4. PATCH - 부분 수정

```java
@PatchMapping("/users/{id}")
public ResponseEntity<UserResponse> patchUser(
        @PathVariable Long id,
        @RequestBody Map<String, Object> updates) {
    
    UserResponse user = userService.patchUser(id, updates);
    return ResponseEntity.ok(user);
}

// 특정 필드 수정
@PatchMapping("/users/{id}/status")
public ResponseEntity<UserResponse> updateUserStatus(
        @PathVariable Long id,
        @RequestBody UserStatusUpdateRequest request) {
    
    UserResponse user = userService.updateUserStatus(id, request.getStatus());
    return ResponseEntity.ok(user);
}
```

### 5. DELETE - 삭제

```java
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}
```

---

## 📊 상태 코드 사용

### 1. 성공 응답

```java
// 200 OK - 조회, 수정 성공
@GetMapping("/users/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(user);
}

// 201 Created - 생성 성공
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
    UserResponse user = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
}

// 204 No Content - 삭제 성공
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}
```

### 2. 클라이언트 오류

```java
// 400 Bad Request - 잘못된 요청
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
    try {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    } catch (ValidationException e) {
        return ResponseEntity.badRequest().build();
    }
}

// 401 Unauthorized - 인증 실패
@GetMapping("/users")
public ResponseEntity<List<UserResponse>> getUsers(HttpServletRequest request) {
    if (!isAuthenticated(request)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    // ...
}

// 403 Forbidden - 권한 없음
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
    if (!hasPermission(request, "DELETE_USER")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    // ...
}

// 404 Not Found - 리소스 없음
@GetMapping("/users/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    try {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    } catch (UserNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}

// 409 Conflict - 충돌
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
    try {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    } catch (DuplicateUserException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
```

### 3. 서버 오류

```java
// 500 Internal Server Error - 서버 오류
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
    log.error("예상치 못한 예외 발생: {}", e.getMessage(), e);
    
    ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("서버 내부 오류가 발생했습니다")
            .timestamp(LocalDateTime.now())
            .build();
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}
```

---

## 📝 요청/응답 형식

### 1. 요청 형식

```java
// ✅ 좋은 예 - 명확한 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2-50자 사이여야 합니다")
    private String username;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    private String name;
}

// ❌ 나쁜 예 - Map 사용
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@RequestBody Map<String, Object> request) {
    // 타입 안전성 없음, 검증 어려움
}
```

### 2. 응답 형식

```java
// ✅ 좋은 예 - 명확한 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String name;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

// ✅ 좋은 예 - 페이징 응답
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
}
```

### 3. 에러 응답 형식

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String code;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
    private String path;
    private String method;
}

// 사용 예시
@ExceptionHandler(ValidationException.class)
public ResponseEntity<ErrorResponse> handleValidationException(
        ValidationException e, HttpServletRequest request) {
    
    ErrorResponse response = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("입력 데이터 검증 실패")
            .details(e.getErrors())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .build();
    
    return ResponseEntity.badRequest().body(response);
}
```

---

## ⚠️ 에러 처리

### 1. 에러 코드 체계

```java
// 에러 코드 규칙: [도메인]_[타입]_[상세]
public enum ErrorCode {
    
    // 사용자 관련 에러
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "사용자가 이미 존재합니다"),
    USER_INVALID_STATUS("USER_INVALID_STATUS", "유효하지 않은 사용자 상태입니다"),
    
    // 인증/인가 관련 에러
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "유효하지 않은 인증 토큰입니다"),
    AUTH_PERMISSION_DENIED("AUTH_PERMISSION_DENIED", "권한이 없습니다"),
    
    // 검증 관련 에러
    VALIDATION_REQUIRED_FIELD("VALIDATION_REQUIRED_FIELD", "필수 필드가 누락되었습니다"),
    VALIDATION_INVALID_FORMAT("VALIDATION_INVALID_FORMAT", "잘못된 형식입니다"),
    VALIDATION_OUT_OF_RANGE("VALIDATION_OUT_OF_RANGE", "범위를 벗어났습니다"),
    
    // 시스템 관련 에러
    SYSTEM_INTERNAL_ERROR("SYSTEM_INTERNAL_ERROR", "시스템 내부 오류가 발생했습니다"),
    SYSTEM_SERVICE_UNAVAILABLE("SYSTEM_SERVICE_UNAVAILABLE", "서비스를 사용할 수 없습니다");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
```

### 2. 전역 예외 처리

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AgenticCpException.class)
    public ResponseEntity<ErrorResponse> handleAgenticCpException(
            AgenticCpException e, HttpServletRequest request) {
        
        log.error("비즈니스 예외 발생: {}", e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        log.error("검증 예외 발생: {}", e.getMessage());
        
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("입력 데이터 검증 실패")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }
}
```

---

## 🔄 버전 관리

### 1. URL 버전 관리

```java
// ✅ 좋은 예 - URL 경로에 버전 포함
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller {
    // v1 API 구현
}

@RestController
@RequestMapping("/api/v2/users")
public class UserV2Controller {
    // v2 API 구현
}

// ❌ 나쁜 예 - 쿼리 파라미터로 버전 관리
@GetMapping("/api/users?version=1")
public ResponseEntity<List<UserResponse>> getUsersV1() {
    // 버전 관리가 어려움
}
```

### 2. 버전별 호환성

```java
// v1 API
@Data
public class UserV1Response {
    private Long id;
    private String username;
    private String email;
    private String name;
    private LocalDateTime createdAt;
}

// v2 API - 새로운 필드 추가
@Data
public class UserV2Response {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String bio;           // 새로 추가된 필드
    private UserStatus status;    // 새로 추가된 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3. 하위 호환성 유지

```java
// v1 API 유지하면서 v2 API 추가
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller {
    
    @GetMapping
    public ResponseEntity<List<UserV1Response>> getUsers() {
        List<User> users = userService.getAllUsers();
        List<UserV1Response> responses = users.stream()
                .map(this::toV1Response)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    private UserV1Response toV1Response(User user) {
        return UserV1Response.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
```

---

## 🔒 보안 고려사항

### 1. 입력 검증

```java
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        // @Valid 어노테이션으로 자동 검증
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable @Positive(message = "사용자 ID는 양수여야 합니다") Long id) {
        // 추가 검증
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}
```

### 2. SQL 인젝션 방지

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ✅ 안전한 방법 - 메서드 이름 기반 쿼리
    Optional<User> findByUsername(String username);
    
    // ✅ 안전한 방법 - @Query with @Param
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.status = :status")
    Optional<User> findByUsernameAndStatus(@Param("username") String username, @Param("status") UserStatus status);
    
    // ❌ 위험한 방법 - 직접 문자열 연결
    // @Query("SELECT u FROM User u WHERE u.username = '" + username + "'") // 절대 사용 금지
}
```

### 3. CORS 설정

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "https://dev.agenticcp.com",
            "https://staging.agenticcp.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

---

## 📋 API 설계 체크리스트

### 설계 단계

- [ ] RESTful 원칙 준수
- [ ] URL 구조 일관성
- [ ] HTTP 메서드 적절한 사용
- [ ] 상태 코드 적절한 사용
- [ ] 요청/응답 형식 정의
- [ ] 에러 처리 방안 수립
- [ ] 버전 관리 전략 수립
- [ ] 보안 고려사항 반영

### 구현 단계

- [ ] 입력 검증 구현
- [ ] 예외 처리 구현
- [ ] 로깅 구현
- [ ] 테스트 코드 작성
- [ ] 문서화 작성
- [ ] 성능 테스트 수행

### 배포 단계

- [ ] API 문서 업데이트
- [ ] 버전 호환성 확인
- [ ] 모니터링 설정
- [ ] 롤백 계획 수립

---

이 API 설계 가이드라인을 준수하여 일관성 있고 사용하기 쉬운 API를 설계하시기 바랍니다.
