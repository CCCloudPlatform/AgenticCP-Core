# Cursor Rules 핵심 규칙 분석 문서

> 기존 개발 가이드라인 문서 6개를 분석하여 Cursor AI에 전달할 핵심 규칙을 추출한 문서입니다.

---

## 📚 분석 대상 문서

1. `CODE_STYLE_GUIDE.md` - 코드 스타일 가이드
2. `DEVELOPMENT_STANDARDS.md` - 개발 표준
3. `API_DESIGN_GUIDELINES.md` - API 설계 가이드라인
4. `EXCEPTION_GUIDLINES.md` - 예외 처리 가이드라인
5. `TESTING_GUIDELINES.md` - 테스트 가이드라인
6. `DOMAIN_ARCHITECTURE.md` - 도메인 아키텍처

---

## 🎯 핵심 규칙 추출 (우선순위별)

### ⭐ High Priority (필수 - 즉시 적용)

#### 1. 프로젝트 기본 정보
```yaml
프로젝트명: AgenticCP-Core
설명: 멀티클라우드 플랫폼 (AWS, GCP, Azure 통합 관리)
아키텍처: 도메인 주도 설계 (DDD), 멀티테넌트
기술스택:
  - Java 17
  - Spring Boot 3.x
  - JPA (Hibernate)
  - MySQL 8.0
  - Redis
  - Maven
```

#### 2. 패키지 구조 (필수)
```
com.agenticcp.core
├── domain/              # 도메인별 패키지 (DDD)
│   ├── user/            # 사용자 도메인
│   │   ├── entity/      # JPA 엔티티
│   │   ├── dto/         # 요청/응답 DTO
│   │   ├── service/     # 비즈니스 로직
│   │   ├── controller/  # REST 컨트롤러
│   │   └── repository/  # 데이터 접근 계층
│   ├── tenant/          # 테넌트 도메인
│   ├── cloud/           # 클라우드 관리
│   ├── security/        # 보안 도메인
│   └── ...
├── common/              # 공통 모듈
│   ├── config/          # 설정 클래스
│   ├── dto/             # 공통 DTO (ApiResponse 등)
│   ├── exception/       # 예외 클래스
│   ├── enums/           # 공통 열거형
│   └── util/            # 유틸리티
├── controller/          # 공통 컨트롤러 (Health, Auth 등)
└── AgenticCpCoreApplication.java
```

#### 3. 네이밍 규칙 (필수)
```yaml
클래스명: PascalCase
  - Entity: User, CloudAccount, SecurityPolicy
  - Service: UserService, CloudAccountService
  - Controller: UserController, CloudAccountController
  - Repository: UserRepository, CloudAccountRepository
  - DTO: UserCreateRequest, UserResponse
  - Exception: UserNotFoundException, BusinessException

메서드명: camelCase
  - 조회: getUserById, findByUsername, getActiveUsers
  - 생성: createUser, save, register
  - 수정: updateUser, modify, change
  - 삭제: deleteUser, remove
  - 검증: validateUser, check, verify
  - 변환: toEntity, toResponse, from

변수명: camelCase
  - userId, userName, userEmail
  - cloudAccount, tenantId
  - NOT: user_id, UserName

상수명: UPPER_SNAKE_CASE
  - MAX_RETRY_COUNT, DEFAULT_PAGE_SIZE, API_VERSION

패키지명: lowercase (단어 구분 없음)
  - com.agenticcp.core.domain.user
  - NOT: com.agenticcp.core.domain.userService

테이블명/컬럼명: snake_case
  - users, cloud_accounts, security_policies
  - user_id, created_at, updated_at
```

#### 4. 의존성 주입 (필수)
```java
// ✅ 권장: 생성자 주입 (Lombok)
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
}

// ✅ 권장: 생성자 주입 (수동)
@Service
public class UserService {
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// ❌ 금지: 필드 주입
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;  // 절대 사용 금지!
}
```

#### 5. API 응답 형식 (필수)
```java
// 모든 API는 ApiResponse<T> 사용
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success(user));
}

// 성공 응답
{
  "success": true,
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00Z"
}

// 에러 응답
{
  "success": false,
  "errorCode": "USER_2001",
  "message": "사용자를 찾을 수 없습니다.",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 6. 예외 처리 (필수)
```java
// BusinessException 계층 사용
public class UserService {
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }
}

// ErrorCategory로 도메인별 에러 코드 관리
public enum ErrorCategory {
    COMMON("COMMON_"),      // 공통: HTTP 상태 코드 그대로
    AUTH("AUTH_"),          // 1000-1999
    USER("USER_"),          // 2000-2999
    TENANT("TENANT_"),      // 3000-3999
    CLOUD("CLOUD_"),        // 4000-4999
    SECURITY("SECURITY_"),  // 5000-5999
    PLATFORM("PLATFORM_"),  // 6000-6999
    COST("COST_"),          // 7000-7999
    MONITORING("MONITORING_"); // 8000-8999
}

// 도메인별 ErrorCode 구현
public enum UserErrorCode implements BaseErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 2002, "이미 사용 중인 이메일입니다."),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, 2003, "유효하지 않은 사용자 상태입니다.");
    
    @Override
    public String getCode() {
        return ErrorCategory.USER.generate(codeNumber); // "USER_2001"
    }
}
```

---

### 🔶 Medium Priority (중요 - 코드 품질)

#### 7. Entity 설계 규칙
```java
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
@EqualsAndHashCode(of = "id")
public class User extends BaseEntity {  // BaseEntity 상속
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50)
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;
    
    @Enumerated(EnumType.STRING)  // ORDINAL 절대 금지!
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 연관관계는 LAZY 로딩
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserProfile> profiles = new ArrayList<>();
}
```

#### 8. Repository 설계 규칙
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 메서드 이름 기반 쿼리 (간단한 경우)
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    
    // @Query 사용 (복잡한 경우)
    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.createdAt DESC")
    List<User> findActiveUsers(@Param("status") UserStatus status);
    
    // 페이징 쿼리
    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatus(@Param("status") UserStatus status, Pageable pageable);
}
```

#### 9. Service 설계 규칙
```java
@Service
@Transactional(readOnly = true)  // 기본은 readOnly
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    // 조회는 readOnly
    public User getUserById(Long userId) {
        log.info("사용자 조회: userId={}", userId);
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }
    
    // 쓰기 작업은 @Transactional
    @Transactional
    public User createUser(UserCreateRequest request) {
        log.info("사용자 생성 시작: username={}", request.getUsername());
        
        // 검증
        validateUserUniqueness(request);
        
        // 생성
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .status(UserStatus.ACTIVE)
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("사용자 생성 완료: userId={}", savedUser.getId());
        
        return savedUser;
    }
}
```

#### 10. Controller 설계 규칙
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("사용자 목록 조회: page={}, size={}", page, size);
        Page<UserResponse> users = userService.getUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable @Positive Long id) {
        
        log.info("사용자 조회: userId={}", id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        
        log.info("사용자 생성: username={}", request.getUsername());
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(user, "사용자가 생성되었습니다."));
    }
}
```

#### 11. DTO 설계 규칙
```java
// 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email
    private String email;
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
    private UserStatus status;
    private LocalDateTime createdAt;
    
    // Entity -> DTO 변환
    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
```

#### 12. 테스트 작성 규칙
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Nested
    @DisplayName("사용자 생성 테스트")
    class CreateUserTest {
        
        @Test
        @DisplayName("정상적인 사용자 생성")
        void createUser_Success() {
            // Given
            UserCreateRequest request = UserCreateRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
            
            User savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .build();
            
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            
            // When
            User result = userService.createUser(request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(userRepository).save(any(User.class));
        }
    }
}
```

---

### 🔷 Low Priority (권장 - 코드 최적화)

#### 13. 로깅 규칙
```java
@Slf4j
public class UserService {
    
    public User createUser(UserCreateRequest request) {
        // INFO: 주요 비즈니스 로직
        log.info("사용자 생성 시작: username={}", request.getUsername());
        
        try {
            // DEBUG: 상세 디버그 정보
            log.debug("사용자 중복 검사: username={}", request.getUsername());
            
            User user = userRepository.save(newUser);
            
            log.info("사용자 생성 완료: userId={}", user.getId());
            return user;
            
        } catch (Exception e) {
            // ERROR: 예외 상황
            log.error("사용자 생성 실패: username={}, error={}", 
                request.getUsername(), e.getMessage(), e);
            throw e;
        }
    }
}
```

#### 14. 주석 작성 규칙
```java
/**
 * 사용자 관리 서비스
 * 
 * <p>사용자의 생성, 조회, 수정, 삭제 기능을 제공합니다.</p>
 * 
 * @author 개발자명
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    /**
     * 사용자 정보를 생성합니다.
     * 
     * @param request 사용자 생성 요청 정보
     * @return 생성된 사용자 정보
     * @throws DuplicateUserException 중복된 사용자명/이메일인 경우
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        // 구현...
    }
}
```

---

## 🚫 금지 사항 (절대 사용 금지)

### 1. 필드 주입 금지
```java
// ❌ 절대 금지
@Autowired
private UserRepository userRepository;

// ✅ 사용
private final UserRepository userRepository;
```

### 2. SELECT * 쿼리 금지
```sql
-- ❌ 절대 금지
SELECT * FROM users;

-- ✅ 사용
SELECT u.id, u.username, u.email FROM users u;
```

### 3. 매직 넘버 하드코딩 금지
```java
// ❌ 절대 금지
if (user.getAge() >= 18) { ... }

// ✅ 사용
private static final int MINIMUM_AGE = 18;
if (user.getAge() >= MINIMUM_AGE) { ... }
```

### 4. System.out.println() 사용 금지
```java
// ❌ 절대 금지
System.out.println("User created: " + userId);

// ✅ 사용
log.info("User created: userId={}", userId);
```

### 5. Enum ORDINAL 사용 금지
```java
// ❌ 절대 금지
@Enumerated(EnumType.ORDINAL)
private UserStatus status;

// ✅ 사용
@Enumerated(EnumType.STRING)
private UserStatus status;
```

### 6. 트랜잭션 없는 쓰기 작업 금지
```java
// ❌ 절대 금지
public User createUser(UserCreateRequest request) {
    return userRepository.save(user);
}

// ✅ 사용
@Transactional
public User createUser(UserCreateRequest request) {
    return userRepository.save(user);
}
```

### 7. Entity를 직접 반환 금지
```java
// ❌ 절대 금지
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
}

// ✅ 사용
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success(user));
}
```

---

## 🎨 코드 스타일 세부 규칙

### 1. 들여쓰기 및 괄호
```java
// ✅ 권장 스타일
public class UserService {
    
    public User createUser(UserCreateRequest request) {
        if (condition) {
            doSomething();
        } else {
            doOtherThing();
        }
        
        return result;
    }
}
```

### 2. import 순서
```java
// 1. Java standard libraries
import java.util.*;
import java.time.*;

// 2. Third-party libraries
import org.springframework.*;
import lombok.*;

// 3. Project classes
import com.agenticcp.core.*;
```

### 3. 공백 사용
```java
// ✅ 연산자 앞뒤 공백
int result = a + b;
boolean isValid = (x > 0) && (y < 10);

// ✅ 콤마 뒤 공백
method(param1, param2, param3);

// ✅ 중괄호 전 공백
if (condition) {
```

---

## 📊 우선순위 요약

| 우선순위 | 영역 | 필수 규칙 수 | 중요도 |
|---------|------|------------|--------|
| High | 프로젝트 기본, 패키지 구조, 네이밍, 의존성 주입, API 응답, 예외 처리 | 6개 | ⭐⭐⭐ |
| Medium | Entity, Repository, Service, Controller, DTO, 테스트 | 6개 | ⭐⭐ |
| Low | 로깅, 주석 | 2개 | ⭐ |
| **금지** | 필드 주입, SELECT *, 매직 넘버 등 | 7개 | 🚫 |

---

## 🎯 Cursor AI 적용 전략

### 1. 모든 규칙을 명확한 예시와 함께 제공
- 좋은 예 (✅) / 나쁜 예 (❌) 명확히 구분
- 실제 프로젝트 코드 스타일 반영

### 2. 도메인 특수 규칙 강조
- 멀티테넌트 아키텍처
- 도메인별 에러 코드 범위
- ApiResponse 일관성

### 3. 금지 사항 명확히 전달
- "절대 사용하지 마세요" 형식으로 강조
- 금지 이유 설명

### 4. 실제 사용 시나리오 포함
- "Product 도메인 추가" 같은 구체적 예시
- Entity → Repository → Service → Controller → DTO 전체 흐름

---

## ✅ 다음 단계

이 분석 문서를 바탕으로 `.cursor/rules/` 디렉토리에 11개의 규칙 파일을 작성합니다:

1. `00-project-overview.md` - 프로젝트 개요
2. `01-code-style.md` - 코드 스타일
3. `02-naming-conventions.md` - 네이밍 규칙
4. `03-package-structure.md` - 패키지 구조
5. `04-api-design.md` - API 설계
6. `05-exception-handling.md` - 예외 처리
7. `06-database-design.md` - 데이터베이스
8. `07-testing.md` - 테스트
9. `08-logging-security.md` - 로깅/보안
10. `09-domain-specific.md` - 도메인 특수 규칙
11. `10-forbidden-practices.md` - 금지 사항

