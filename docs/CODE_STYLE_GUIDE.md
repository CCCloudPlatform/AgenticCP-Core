# AgenticCP 코드 스타일 가이드

## 📋 목차

1. [Java 코드 스타일](#java-코드-스타일)
2. [Spring Boot 스타일](#spring-boot-스타일)
3. [데이터베이스 스타일](#데이터베이스-스타일)
4. [API 스타일](#api-스타일)
5. [테스트 스타일](#테스트-스타일)
6. [문서화 스타일](#문서화-스타일)

---

## ☕ Java 코드 스타일

### 1. 클래스 선언

```java
// ✅ 좋은 예
/**
 * 사용자 관리 서비스
 * 
 * @author 개발자명
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class UserService {
    
    private static final String DEFAULT_STATUS = "ACTIVE";
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    // 메서드들...
}

// ❌ 나쁜 예
@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    EmailService emailService;
    
    // 메서드들...
}
```

### 2. 메서드 선언

```java
// ✅ 좋은 예
/**
 * 사용자 정보를 생성합니다.
 * 
 * @param request 사용자 생성 요청 정보
 * @return 생성된 사용자 정보
 * @throws DuplicateUserException 중복된 사용자명 또는 이메일인 경우
 */
@Transactional
public User createUser(UserCreateRequest request) {
    log.info("사용자 생성 시작: username={}", request.getUsername());
    
    validateUserUniqueness(request);
    
    User user = buildUser(request);
    User savedUser = userRepository.save(user);
    
    emailService.sendWelcomeEmail(savedUser);
    
    log.info("사용자 생성 완료: userId={}", savedUser.getId());
    return savedUser;
}

// ❌ 나쁜 예
@Transactional
public User createUser(UserCreateRequest request) {
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    return userRepository.save(user);
}
```

### 3. 변수 선언

```java
// ✅ 좋은 예
public void processUsers(List<User> users) {
    List<User> activeUsers = users.stream()
            .filter(user -> UserStatus.ACTIVE.equals(user.getStatus()))
            .collect(Collectors.toList());
    
    for (User user : activeUsers) {
        processUser(user);
    }
}

// ❌ 나쁜 예
public void processUsers(List<User> users) {
    List<User> activeUsers = new ArrayList<>();
    for (User user : users) {
        if (user.getStatus().equals("ACTIVE")) {
            activeUsers.add(user);
        }
    }
    for (User user : activeUsers) {
        processUser(user);
    }
}
```

### 4. 조건문과 반복문

```java
// ✅ 좋은 예
public Optional<User> findUserByEmail(String email) {
    if (StringUtils.isBlank(email)) {
        log.warn("빈 이메일로 사용자 검색 시도");
        return Optional.empty();
    }
    
    return userRepository.findByEmail(email);
}

// ❌ 나쁜 예
public Optional<User> findUserByEmail(String email) {
    if (email == null || email.equals("")) {
        return Optional.empty();
    }
    return userRepository.findByEmail(email);
}
```

---

## 🌱 Spring Boot 스타일

### 1. 의존성 주입

```java
// ✅ 좋은 예 - 생성자 주입
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
}

// ✅ 좋은 예 - Lombok 사용
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
}

// ❌ 나쁜 예 - 필드 주입
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
}
```

### 2. 트랜잭션 관리

```java
// ✅ 좋은 예
@Service
@Transactional(readOnly = true)
public class UserService {
    
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @Transactional
    public User createUser(UserCreateRequest request) {
        // 쓰기 작업
        return userRepository.save(user);
    }
    
    @Transactional
    public void updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        
        user.updateFrom(request);
        userRepository.save(user);
    }
}

// ❌ 나쁜 예
@Service
public class UserService {
    
    @Transactional
    public Optional<User> getUserById(Long id) { // 읽기 전용인데 @Transactional 사용
        return userRepository.findById(id);
    }
}
```

### 3. 설정 클래스

```java
// ✅ 좋은 예
@Configuration
@EnableJpaRepositories(basePackages = "com.agenticcp.core.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

---

## 🗄️ 데이터베이스 스타일

### 1. 엔티티 설계

```java
// ✅ 좋은 예
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_status_created_at", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "profiles")
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
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserProfile> profiles = new ArrayList<>();
    
    // 비즈니스 메서드
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
    
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
    
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
}
```

### 2. Repository 설계

```java
// ✅ 좋은 예
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
     * 이메일 중복 검사
     */
    boolean existsByEmail(String email);
    
    /**
     * 복잡한 쿼리
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
     * 페이징 쿼리
     */
    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findActiveUsersWithPagination(
        @Param("status") UserStatus status,
        Pageable pageable
    );
}
```

---

## 🌐 API 스타일

### 1. 컨트롤러 설계

```java
// ✅ 좋은 예
@RestController
@RequestMapping("/api/v1/users")
@Validated
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        log.info("사용자 목록 조회: page={}, size={}, status={}", page, size, status);
        
        Page<UserResponse> users = userService.getUsers(page, size, status);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable @Positive(message = "사용자 ID는 양수여야 합니다") Long id) {
        
        log.info("사용자 조회: userId={}", id);
        
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        
        log.info("사용자 생성: username={}", request.getUsername());
        
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("사용자 수정: userId={}", id);
        
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Positive Long id) {
        
        log.info("사용자 삭제: userId={}", id);
        
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 2. DTO 설계

```java
// ✅ 좋은 예 - 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserCreateRequest {
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2-50자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 언더스코어만 사용 가능합니다")
    private String username;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다")
    private String email;
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    private String name;
    
    @Size(max = 500, message = "자기소개는 500자를 초과할 수 없습니다")
    private String bio;
}

// ✅ 좋은 예 - 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String name;
    private String bio;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .bio(user.getBio())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
```

---

## 🧪 테스트 스타일

### 1. 단위 테스트

```java
// ✅ 좋은 예
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
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
                    .name("테스트 사용자")
                    .build();
            
            User savedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .status(UserStatus.ACTIVE)
                    .build();
            
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            
            // When
            User result = userService.createUser(request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            
            verify(userRepository).save(any(User.class));
            verify(emailService).sendWelcomeEmail(savedUser);
        }
        
        @Test
        @DisplayName("중복된 사용자명으로 생성 시 예외 발생")
        void createUser_DuplicateUsername_ThrowsException() {
            // Given
            UserCreateRequest request = UserCreateRequest.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .build();
            
            when(userRepository.existsByUsername("testuser")).thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateUserException.class)
                    .hasMessage("사용자명이 이미 존재합니다: testuser");
            
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendWelcomeEmail(any(User.class));
        }
    }
}
```

### 2. 통합 테스트

```java
// ✅ 좋은 예
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
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
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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
        assertThat(response.getBody().getEmail()).isEqualTo("api@example.com");
        
        // 데이터베이스 검증
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("apiuser");
    }
    
    @Test
    @DisplayName("사용자 조회 API 테스트")
    void getUser_Success() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);
        
        // When
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                "/api/v1/users/" + savedUser.getId(), UserResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }
}
```

---

## 📚 문서화 스타일

### 1. JavaDoc 작성

```java
/**
 * 사용자 관리 서비스
 * 
 * <p>사용자의 생성, 조회, 수정, 삭제 기능을 제공합니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * UserCreateRequest request = UserCreateRequest.builder()
 *     .username("testuser")
 *     .email("test@example.com")
 *     .name("테스트 사용자")
 *     .build();
 * 
 * User user = userService.createUser(request);
 * }</pre>
 * 
 * @author 개발자명
 * @version 1.0.0
 * @since 2024-01-01
 * @see User
 * @see UserRepository
 */
@Service
@Transactional(readOnly = true)
public class UserService {
    
    /**
     * 사용자 정보를 생성합니다.
     * 
     * <p>사용자명과 이메일의 중복을 검사한 후 사용자를 생성합니다.
     * 생성된 사용자에게 환영 이메일을 발송합니다.</p>
     * 
     * @param request 사용자 생성 요청 정보 (null이면 안됨)
     * @return 생성된 사용자 정보
     * @throws DuplicateUserException 사용자명 또는 이메일이 중복인 경우
     * @throws ValidationException 요청 데이터가 유효하지 않은 경우
     * @see UserCreateRequest
     * @see DuplicateUserException
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        // 구현...
    }
}
```

### 2. README 작성

```markdown
# UserService

사용자 관리 서비스를 제공하는 클래스입니다.

## 주요 기능

- 사용자 생성
- 사용자 조회
- 사용자 수정
- 사용자 삭제

## 사용 방법

```java
@Autowired
private UserService userService;

// 사용자 생성
UserCreateRequest request = UserCreateRequest.builder()
    .username("testuser")
    .email("test@example.com")
    .name("테스트 사용자")
    .build();

User user = userService.createUser(request);
```

## 주의사항

- 사용자명은 2-50자 사이여야 합니다
- 이메일은 유효한 형식이어야 합니다
- 중복된 사용자명이나 이메일은 허용되지 않습니다
```

---

## 📋 체크리스트

### 코드 작성 전 체크리스트

- [ ] 요구사항 명확히 파악
- [ ] 설계 문서 검토
- [ ] 기존 코드 스타일 확인
- [ ] 테스트 케이스 계획

### 코드 작성 중 체크리스트

- [ ] 네이밍 규칙 준수
- [ ] 적절한 주석 작성
- [ ] 예외 처리 구현
- [ ] 로깅 구현
- [ ] 단위 테스트 작성

### 코드 작성 후 체크리스트

- [ ] 코드 리뷰 요청
- [ ] 통합 테스트 실행
- [ ] 문서 업데이트
- [ ] 성능 검증

---

이 코드 스타일 가이드를 준수하여 일관성 있고 읽기 쉬운 코드를 작성하시기 바랍니다.
