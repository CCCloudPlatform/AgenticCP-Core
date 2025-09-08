# AgenticCP 개발 표준

## 📋 목차

1. [프로젝트 구조](#프로젝트-구조)
2. [코드 스타일 가이드](#코드-스타일-가이드)
3. [JPA 사용 표준](#jpa-사용-표준)
4. [SQL 작성 표준](#sql-작성-표준)
5. [REST API 표준](#rest-api-표준)
6. [크로스 도메인 처리](#크로스-도메인-처리)
7. [테스트 코드 표준](#테스트-코드-표준)
8. [예외 처리 표준](#예외-처리-표준)
9. [로깅 표준](#로깅-표준)
10. [보안 표준](#보안-표준)

---

## 🏗️ 프로젝트 구조

### 패키지 구조
```
com.agenticcp.core
├── config/          # 설정 클래스
├── controller/      # REST 컨트롤러
├── service/         # 비즈니스 로직
├── repository/      # 데이터 접근 계층
├── entity/          # JPA 엔티티
├── dto/            # 데이터 전송 객체
├── exception/      # 예외 클래스
├── util/           # 유틸리티 클래스
└── common/         # 공통 상수, 열거형
```

### 네이밍 규칙
- **패키지**: 소문자, 단어 구분 없음 (`com.agenticcp.core.user`)
- **클래스**: PascalCase (`UserController`, `UserService`)
- **메서드**: camelCase (`getUserById`, `createUser`)
- **변수**: camelCase (`userId`, `userName`)
- **상수**: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **테이블**: snake_case (`user_profiles`, `order_items`)

---

## 🎨 코드 스타일 가이드

### 1. 클래스 작성 규칙

```java
/**
 * 사용자 관리 서비스
 * 
 * @author 개발자명
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@Transactional(readOnly = true)
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    // 메서드 구현...
}
```

### 2. 메서드 작성 규칙

```java
/**
 * 사용자 ID로 사용자 정보를 조회합니다.
 * 
 * @param userId 조회할 사용자 ID
 * @return 사용자 정보 (Optional)
 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
 */
@Transactional(readOnly = true)
public Optional<User> getUserById(Long userId) {
    log.debug("사용자 조회 시작: userId={}", userId);
    
    if (userId == null || userId <= 0) {
        throw new IllegalArgumentException("유효하지 않은 사용자 ID: " + userId);
    }
    
    Optional<User> user = userRepository.findById(userId);
    log.debug("사용자 조회 완료: userId={}, found={}", userId, user.isPresent());
    
    return user;
}
```

### 3. 주석 작성 규칙

```java
// ✅ 좋은 예
/**
 * 사용자 정보를 생성합니다.
 * 
 * @param userCreateRequest 사용자 생성 요청 정보
 * @return 생성된 사용자 정보
 * @throws DuplicateUserException 중복된 사용자명 또는 이메일인 경우
 */
public User createUser(UserCreateRequest userCreateRequest) {
    // 중복 검증
    validateUserUniqueness(userCreateRequest);
    
    // 사용자 생성
    User user = new User();
    user.setUsername(userCreateRequest.getUsername());
    // ... 나머지 설정
    
    return userRepository.save(user);
}

// ❌ 나쁜 예
public User createUser(UserCreateRequest request) { // 주석 없음
    User user = new User(); // 의미 없는 주석
    user.setUsername(request.getUsername());
    return userRepository.save(user);
}
```

---

## 🗄️ JPA 사용 표준

### 1. 엔티티 작성 규칙

```java
/**
 * 사용자 엔티티
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2-50자 사이여야 합니다")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 연관관계 매핑
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserProfile> profiles = new ArrayList<>();
}
```

### 2. Repository 작성 규칙

```java
/**
 * 사용자 데이터 접근 계층
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 활성 사용자 목록 조회
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.createdAt DESC")
    List<User> findActiveUsers();
    
    /**
     * 사용자명 중복 검사
     */
    boolean existsByUsername(String username);
    
    /**
     * 복잡한 쿼리는 @Query 사용
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.status = :status 
        AND u.createdAt BETWEEN :startDate AND :endDate
        ORDER BY u.createdAt DESC
        """)
    List<User> findUsersByStatusAndDateRange(
        @Param("status") UserStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 네이티브 쿼리 사용 시
     */
    @Query(value = """
        SELECT u.*, COUNT(p.id) as profile_count 
        FROM users u 
        LEFT JOIN user_profiles p ON u.id = p.user_id 
        WHERE u.status = :status 
        GROUP BY u.id
        """, nativeQuery = true)
    List<Object[]> findUsersWithProfileCount(@Param("status") String status);
}
```

### 3. JPA 사용 가이드라인

#### ✅ 권장사항
- `@Transactional(readOnly = true)`를 기본으로 사용
- `FetchType.LAZY`를 기본으로 사용
- `@CreationTimestamp`, `@UpdateTimestamp` 사용
- 복합 인덱스는 `@Index` 어노테이션으로 명시
- 검증 어노테이션을 엔티티에 적용

#### ❌ 금지사항
- N+1 문제를 일으키는 즉시 로딩
- `@Transactional`을 컨트롤러에 사용
- 하드코딩된 쿼리 문자열
- 무분별한 `@Transactional` 사용

---

## 📊 SQL 작성 표준

### 1. 테이블 생성 규칙

```sql
-- ✅ 좋은 예
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID',
    username VARCHAR(50) NOT NULL COMMENT '사용자명',
    email VARCHAR(255) NOT NULL COMMENT '이메일',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE' COMMENT '상태',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_status (status),
    INDEX idx_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정보';
```

### 2. 쿼리 작성 규칙

```sql
-- ✅ 좋은 예
SELECT 
    u.id,
    u.username,
    u.email,
    u.status,
    u.created_at
FROM users u
WHERE u.status = 'ACTIVE'
  AND u.created_at >= '2024-01-01'
ORDER BY u.created_at DESC
LIMIT 10;

-- ❌ 나쁜 예
SELECT * FROM users WHERE status = 'ACTIVE'; -- SELECT *, 정렬 없음
```

### 3. 인덱스 설계 원칙

```sql
-- 복합 인덱스 예시
CREATE INDEX idx_users_status_created_at ON users (status, created_at);

-- 부분 인덱스 예시 (MySQL 8.0+)
CREATE INDEX idx_users_active_created_at ON users (created_at) WHERE status = 'ACTIVE';
```

---

## 🌐 REST API 표준

### 1. URL 설계 규칙

```
# 리소스 기반 URL
GET    /api/v1/users              # 사용자 목록 조회
GET    /api/v1/users/{id}         # 특정 사용자 조회
POST   /api/v1/users              # 사용자 생성
PUT    /api/v1/users/{id}         # 사용자 전체 수정
PATCH  /api/v1/users/{id}         # 사용자 부분 수정
DELETE /api/v1/users/{id}         # 사용자 삭제

# 하위 리소스
GET    /api/v1/users/{id}/profiles    # 사용자의 프로필 목록
POST   /api/v1/users/{id}/profiles    # 사용자 프로필 생성

# 액션 기반 URL
POST   /api/v1/users/{id}/activate    # 사용자 활성화
POST   /api/v1/users/{id}/deactivate  # 사용자 비활성화
```

### 2. HTTP 상태 코드 사용

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users); // 200 OK
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user); // 201 Created
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return user != null ? 
            ResponseEntity.ok(user) : 
            ResponseEntity.notFound().build(); // 404 Not Found
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            UserResponse user = userService.updateUser(id, request);
            return ResponseEntity.ok(user); // 200 OK
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }
    }
}
```

### 3. 요청/응답 DTO 설계

```java
// 요청 DTO
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

// 응답 DTO
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
}

// 에러 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String code;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
}
```

---

## 🌍 크로스 도메인 처리

### 1. CORS 설정

```java
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",  // 개발 환경
                    "https://dev.agenticcp.com",  // 개발 서버
                    "https://staging.agenticcp.com"  // 스테이징 서버
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 2. 환경별 CORS 설정

```yaml
# application.yml
cors:
  allowed-origins:
    local: "http://localhost:3000,http://localhost:8080"
    dev: "https://dev.agenticcp.com"
    staging: "https://staging.agenticcp.com"
    prod: "https://agenticcp.com"
```

```java
@Configuration
public class CorsConfig {
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
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

### 3. 컨트롤러 레벨 CORS 설정

```java
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(
    origins = {"http://localhost:3000", "https://dev.agenticcp.com"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT},
    allowedHeaders = "*",
    allowCredentials = "true"
)
public class UserController {
    // 컨트롤러 구현...
}
```

---

## 🧪 테스트 코드 표준

### 1. 테스트 클래스 구조

```java
@SpringBootTest
@Transactional
@Rollback
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @MockBean
    private EmailService emailService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE)
                .build();
    }
    
    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }
    
    @Nested
    @DisplayName("사용자 생성 테스트")
    class CreateUserTest {
        
        @Test
        @DisplayName("정상적인 사용자 생성")
        void createUser_Success() {
            // Given
            UserCreateRequest request = UserCreateRequest.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .name("새 사용자")
                    .build();
            
            // When
            User result = userService.createUser(request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
        
        @Test
        @DisplayName("중복된 사용자명으로 생성 시 예외 발생")
        void createUser_DuplicateUsername_ThrowsException() {
            // Given
            userRepository.save(testUser);
            UserCreateRequest request = UserCreateRequest.builder()
                    .username("testuser")  // 중복된 사용자명
                    .email("different@example.com")
                    .name("다른 사용자")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateUserException.class)
                    .hasMessage("사용자명이 이미 존재합니다: testuser");
        }
    }
    
    @Nested
    @DisplayName("사용자 조회 테스트")
    class GetUserTest {
        
        @Test
        @DisplayName("존재하는 사용자 조회")
        void getUserById_ExistingUser_ReturnsUser() {
            // Given
            User savedUser = userRepository.save(testUser);
            
            // When
            Optional<User> result = userService.getUserById(savedUser.getId());
            
            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }
        
        @Test
        @DisplayName("존재하지 않는 사용자 조회")
        void getUserById_NonExistingUser_ReturnsEmpty() {
            // When
            Optional<User> result = userService.getUserById(999L);
            
            // Then
            assertThat(result).isEmpty();
        }
    }
}
```

### 2. 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserControllerIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Test
    @DisplayName("사용자 생성 API 테스트")
    void createUser_Success() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .username("apiuser")
                .email("api@example.com")
                .name("API 사용자")
                .build();
        
        // When
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/v1/users", request, UserResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("apiuser");
    }
}
```

### 3. 테스트 데이터 관리

```java
@Component
public class TestDataBuilder {
    
    public static User.UserBuilder userBuilder() {
        return User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE);
    }
    
    public static UserCreateRequest.UserCreateRequestBuilder userCreateRequestBuilder() {
        return UserCreateRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .name("새 사용자");
    }
}

// 테스트에서 사용
@Test
void testWithBuilder() {
    User user = TestDataBuilder.userBuilder()
            .username("customuser")
            .build();
    
    UserCreateRequest request = TestDataBuilder.userCreateRequestBuilder()
            .username("customuser")
            .build();
}
```

---

## ⚠️ 예외 처리 표준

### 1. 커스텀 예외 클래스

```java
// 기본 예외 클래스
public abstract class AgenticCpException extends RuntimeException {
    
    private final String code;
    private final HttpStatus httpStatus;
    
    protected AgenticCpException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    protected AgenticCpException(String code, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}

// 구체적인 예외 클래스들
public class UserNotFoundException extends AgenticCpException {
    public UserNotFoundException(Long userId) {
        super("USER_NOT_FOUND", "사용자를 찾을 수 없습니다: " + userId, HttpStatus.NOT_FOUND);
    }
}

public class DuplicateUserException extends AgenticCpException {
    public DuplicateUserException(String field, String value) {
        super("DUPLICATE_USER", field + "이 이미 존재합니다: " + value, HttpStatus.CONFLICT);
    }
}

public class ValidationException extends AgenticCpException {
    private final List<String> errors;
    
    public ValidationException(List<String> errors) {
        super("VALIDATION_ERROR", "입력 데이터 검증 실패", HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }
    
    public List<String> getErrors() { return errors; }
}
```

### 2. 전역 예외 처리기

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AgenticCpException.class)
    public ResponseEntity<ErrorResponse> handleAgenticCpException(AgenticCpException e) {
        log.error("비즈니스 예외 발생: {}", e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
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
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }
    
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
}
```

---

## 📝 로깅 표준

### 1. 로그 레벨 사용 가이드

```java
@Service
@Slf4j
public class UserService {
    
    public User createUser(UserCreateRequest request) {
        log.info("사용자 생성 시작: username={}", request.getUsername());
        
        try {
            // 중복 검증
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("중복된 사용자명으로 생성 시도: username={}", request.getUsername());
                throw new DuplicateUserException("username", request.getUsername());
            }
            
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            
            User savedUser = userRepository.save(user);
            log.info("사용자 생성 완료: userId={}, username={}", savedUser.getId(), savedUser.getUsername());
            
            return savedUser;
            
        } catch (Exception e) {
            log.error("사용자 생성 실패: username={}, error={}", request.getUsername(), e.getMessage(), e);
            throw e;
        }
    }
    
    public Optional<User> getUserById(Long userId) {
        log.debug("사용자 조회: userId={}", userId);
        
        Optional<User> user = userRepository.findById(userId);
        
        if (user.isPresent()) {
            log.debug("사용자 조회 성공: userId={}, username={}", userId, user.get().getUsername());
        } else {
            log.debug("사용자 조회 실패: userId={}", userId);
        }
        
        return user;
    }
}
```

### 2. 로그 설정

```yaml
# application.yml
logging:
  level:
    com.agenticcp: INFO
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/agenticcp-core.log
    max-size: 100MB
    max-history: 30
```

---

## 🔒 보안 표준

### 1. 입력 데이터 검증

```java
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        // @Valid 어노테이션으로 자동 검증
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable @Positive(message = "사용자 ID는 양수여야 합니다") Long id) {
        // @Positive 어노테이션으로 추가 검증
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponse.from(user));
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

### 3. 패스워드 보안 (참고용)

```java
@Service
public class PasswordService {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

---

## 📋 체크리스트

### 코드 리뷰 체크리스트

- [ ] 네이밍 규칙 준수
- [ ] 적절한 주석 작성
- [ ] 예외 처리 구현
- [ ] 로깅 구현
- [ ] 테스트 코드 작성
- [ ] 보안 검증 구현
- [ ] 성능 고려사항 반영
- [ ] 코드 중복 제거

### 배포 전 체크리스트

- [ ] 모든 테스트 통과
- [ ] 코드 리뷰 완료
- [ ] 보안 검증 완료
- [ ] 성능 테스트 완료
- [ ] 문서 업데이트 완료

---

이 개발 표준을 준수하여 일관성 있고 유지보수가 용이한 코드를 작성하시기 바랍니다.
