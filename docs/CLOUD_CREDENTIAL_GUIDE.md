# Cloud Domain 자격증명 관리 가이드라인

## 목차

1. [개요](#개요)
2. [엔티티 구조](#엔티티-구조)
3. [자격증명 등록 로직](#자격증명-등록-로직)
4. [단기 세션 획득 로직](#단기-세션-획득-로직)
5. [CloudResourceUseCase에서 자격증명 해결 방법](#cloudresourceusecase에서-자격증명-해결-방법)
6. [모범 사례](#모범-사례)

---

## 개요

Cloud Domain에서는 클라우드 프로바이더(AWS, Azure, GCP 등)의 자격증명을 안전하게 관리하고, 리소스 작업 시 필요한 자격증명을 동적으로 해결합니다.

### 핵심 개념

- **장기 자격증명 (Long-term Credentials)**: 사용자가 제공한 Access Key, Secret Key 등
- **단기 세션 (Short-term Session)**: STS, OAuth2 토큰 등 임시 자격증명
- **JIT (Just-In-Time)**: 작업 수행 시점에 필요한 자격증명을 동적으로 획득
- **자격증명 암호화**: 모든 자격증명은 암호화되어 저장됨

---

## 엔티티 구조

### CloudAccount 엔티티

클라우드 계정 정보를 관리하는 메인 엔티티입니다.

```java
@Entity
@Table(name = "cloud_accounts")
public class CloudAccount extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private CloudProvider provider;
    
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;
    
    @Column(name = "account_scope", length = 100)
    private String accountScope;  // AWS: Account ID, Azure: Subscription ID, GCP: Project ID
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "credential_id", nullable = false, unique = true)
    private CloudAccountCredential credential;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
```

**주요 필드 설명:**

- `tenant`: 멀티 테넌트 지원을 위한 테넌트 참조
- `provider`: 클라우드 프로바이더 (AWS, Azure, GCP 등)
- `accountName`: 사용자가 지정한 계정 이름
- `accountScope`: 프로바이더별 계정 식별자
  - AWS: Account ID (예: `123456789012`)
  - Azure: Subscription ID (예: `abc123-def456-...`)
  - GCP: Project ID (예: `my-project-123`)
- `credential`: 암호화된 자격증명 참조 (OneToOne 관계)
- `accountStatus`: 계정 상태 (ACTIVE, INACTIVE, VERIFIED, SUSPENDED, FAILED)
- `isDefault`: 프로바이더별 기본 계정 여부

### CloudAccountCredential 엔티티

암호화된 자격증명을 저장하는 별도 엔티티입니다. 보안을 위해 메인 테이블과 분리되어 있습니다.

```java
@Entity
@Table(name = "cloud_account_credentials")
public class CloudAccountCredential extends BaseEntity {
    @Column(name = "credential_key", nullable = false, unique = true, length = 100)
    private String credentialKey;  // UUID 기반 고유 키
    
    @Column(name = "access_key_id_encrypted", columnDefinition = "TEXT", nullable = false)
    private String accessKeyIdEncrypted;  // 암호화된 Access Key ID
    
    @Column(name = "secret_access_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String secretAccessKeyEncrypted;  // 암호화된 Secret Access Key
    
    @Column(name = "region", length = 50)
    private String region;  // 기본 리전
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // 추가 메타데이터 (JSON)
}
```

**주요 필드 설명:**

- `credentialKey`: UUID 기반 고유 키로, 외부에서 자격증명을 참조할 때 사용
- `accessKeyIdEncrypted`: 암호화된 Access Key ID
  - AWS: Access Key ID
  - Azure: Client ID
  - GCP: Service Account Key
- `secretAccessKeyEncrypted`: 암호화된 Secret Access Key
  - AWS: Secret Access Key
  - Azure: Client Secret
  - GCP: Service Account Secret
- `region`: 프로바이더별 기본 리전 정보

**보안 고려사항:**

- 모든 자격증명은 `EncryptionService`를 통해 암호화되어 저장됨
- 평문 자격증명은 메모리에만 존재하며, DB에는 저장되지 않음
- `CloudAccount`와 `CloudAccountCredential`은 OneToOne 관계로 강하게 결합되어 있음

---

## 자격증명 등록 로직

### 전체 흐름

```
1. 계정 등록 요청 (RegisterCloudAccountRequest)
   ↓
2. 테넌트 및 프로바이더 조회
   ↓
3. 계정 중복 검증 (Domain Service)
   ↓
4. 자격증명 검증 (AccountValidationPort)
   ↓
5. 자격증명 암호화 저장 (AccountCredentialManagementPort)
   ↓
6. CloudAccount 엔티티 생성 및 저장
   ↓
7. 감사 로그 기록
```

### 구현 예시

```java
@Transactional
public CloudAccountDto registerCloudAccount(RegisterCloudAccountRequest request) {
    String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
    
    // 1. 테넌트 및 프로바이더 조회
    Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
            .orElseThrow(() -> new BusinessException(...));
    
    CloudProvider provider = cloudProviderRepository.findByProviderType(request.getProviderType())
            .orElseThrow(() -> new BusinessException(...));
    
    // 2. 계정 중복 검증
    cloudAccountDomainService.validateAccountUniqueness(
        tenant.getId(), request.getAccountScope(), request.getProviderType());
    
    // 3. 자격증명 검증
    AccountValidationRequest validationRequest = AccountValidationRequest.builder()
            .providerType(request.getProviderType())
            .accessKeyId(request.getAccessKey())
            .secretAccessKey(request.getSecretKey())
            .region(request.getRegion())
            .build();
    
    AccountValidationResult validationResult = accountValidationPort.validateAccount(validationRequest);
    
    if (!validationResult.getValid()) {
        throw new BusinessException(
            CloudErrorCode.ACCOUNT_VERIFICATION_FAILED, 
            validationResult.getMessage()
        );
    }
    
    // 4. 자격증명 암호화 저장
    StoreCredentialCommand storeCommand = credentialCommandMapper.toStoreCommand(
            tenant.getTenantKey(),
            request.getProviderType(),
            request.getAccountScope() != null ? request.getAccountScope() : validationResult.getAccountScope(),
            request.getAccessKey(),
            request.getSecretKey(),
            request.getRegion() != null ? request.getRegion() : validationResult.getRegion()
    );
    
    String credentialKey = accountCredentialManagementPort.storeCredentials(
            storeCommand.getTenantKey(),
            storeCommand.getProviderType(),
            storeCommand.getAccountScope(),
            storeCommand.getCredentials()
    );
    
    // 5. CloudAccountCredential 엔티티 조회
    CloudAccountCredential savedCredential = cloudAccountCredentialRepository.findByCredentialKey(credentialKey)
            .orElseThrow(() -> new BusinessException(...));
    
    // 6. CloudAccount 엔티티 생성 및 저장
    CloudAccount cloudAccount = CloudAccount.builder()
            .tenant(tenant)
            .provider(provider)
            .accountName(request.getAccountName())
            .accountScope(validationResult.getAccountScope() != null ? 
                         validationResult.getAccountScope() : request.getAccountScope())
            .credential(savedCredential)
            .accountStatus(AccountStatus.VERIFIED)
            .isDefault(request.getIsDefault() != null && request.getIsDefault())
            .verifiedAt(LocalDateTime.now())
            .build();
    
    CloudAccount savedAccount = cloudAccountRepository.save(cloudAccount);
    
    return CloudAccountMapper.toDto(savedAccount);
}
```

### AccountCredentialManagementPort.storeCredentials() 내부 동작

```java
// AwsAccountCredentialManagementAdapter
@Override
public String storeCredentials(String tenantKey, ProviderType providerType, 
                               String accountScope, Map<String, String> credentials) {
    String accessKey = credentials.get("accessKeyId");
    String secretKey = credentials.get("secretAccessKey");
    String region = credentials.get("region");
    
    return awsCredentialManager.storeCredentials(tenantKey, accessKey, secretKey, region)
            .getCredentialKey();
}
```

```java
// AwsCredentialManager
@Transactional
public CloudAccountCredential storeCredentials(String tenantKey, String accessKeyId,
                                               String secretAccessKey, String region) {
    // 1. 자격증명 암호화
    String encryptedAccessKeyId = encryptionService.encrypt(accessKeyId);
    String encryptedSecretAccessKey = encryptionService.encrypt(secretAccessKey);
    
    // 2. UUID 기반 credentialKey 생성
    String credentialKey = UUID.randomUUID().toString();
    
    // 3. CloudAccountCredential 엔티티 생성 및 저장
    CloudAccountCredential credential = CloudAccountCredential.builder()
            .credentialKey(credentialKey)
            .accessKeyIdEncrypted(encryptedAccessKeyId)
            .secretAccessKeyEncrypted(encryptedSecretAccessKey)
            .region(region)
            .build();
    
    return credentialRepository.save(credential);
}
```

### 주요 포인트

1. **검증 후 저장**: 자격증명을 저장하기 전에 반드시 검증을 수행합니다.
2. **암호화**: 모든 자격증명은 `EncryptionService`를 통해 암호화되어 저장됩니다.
3. **UUID 기반 키**: `credentialKey`는 UUID로 생성되어 외부 참조에 사용됩니다.
4. **트랜잭션 관리**: `@Transactional`을 통해 원자성을 보장합니다.

---

## 단기 세션 획득 로직

### 개요

장기 자격증명을 사용하여 단기 세션(STS 토큰, OAuth2 토큰 등)을 JIT(Just-In-Time) 방식으로 획득합니다.

### 전체 흐름

```
1. getSession() 호출
   ↓
2. 캐시된 세션 확인 (SessionCacheService)
   ↓
3-1. 캐시된 세션이 있고 유효하면 → 캐시된 세션 반환
   ↓
3-2. 캐시된 세션이 없거나 만료되었으면
   ↓
4. credentialKey 조회 (CloudAccount → CloudAccountCredential)
   ↓
5. 장기 자격증명 복호화 (AwsCredentialManager)
   ↓
6. STS 세션 발급 (AwsSessionProvider)
   ↓
7. 세션 캐싱 (SessionCacheService)
   ↓
8. 세션 반환
```

### 구현 예시

```java
// AccountCredentialManagementPort
@Override
public CloudSessionCredential getSession(String tenantKey, String accountScope, ProviderType providerType) {
    // 1. 캐시된 세션 확인
    Optional<CloudSessionCredential> cachedSession = sessionCacheService.getCachedSession(
            tenantKey, accountScope, providerType);
    
    if (cachedSession.isPresent() && cachedSession.get().isValid()) {
        log.debug("캐시된 세션 사용");
        return cachedSession.get();
    }
    
    // 2. credentialKey 조회
    String credentialKey = findCredentialKey(tenantKey, providerType, accountScope);
    
    // 3. STS 세션 발급
    CloudSessionCredential session = awsSessionProvider.getSession(
            credentialKey, DEFAULT_SESSION_DURATION_SECONDS);
    
    // 4. 세션 캐싱
    int ttlMinutes = calculateTtlMinutes(session);
    sessionCacheService.cacheSession(tenantKey, accountScope, providerType, session, ttlMinutes);
    
    return session;
}
```

### AwsSessionProvider.getSession() 내부 동작

```java
public CloudSessionCredential getSession(String credentialKey, int durationSeconds) {
    // 1. 장기 자격증명 조회 및 복호화
    AwsCredentials longTermCredentials = awsCredentialManager.getCredentials(credentialKey);
    
    // 2. STS Client 생성
    StsClient stsClient = StsClient.builder()
            .credentialsProvider(() -> 
                AwsBasicCredentials.create(
                    longTermCredentials.getAccessKeyId(),
                    longTermCredentials.getSecretAccessKey()
                ))
            .region(Region.of(longTermCredentials.getRegion() != null ? 
                             longTermCredentials.getRegion() : "us-east-1"))
            .build();
    
    try {
        // 3. GetSessionToken 요청
        GetSessionTokenRequest request = GetSessionTokenRequest.builder()
                .durationSeconds(Math.min(durationSeconds, 43200)) // 최대 12시간
                .build();
        
        GetSessionTokenResponse response = stsClient.getSessionToken(request);
        
        // 4. CloudSessionCredential로 변환
        CloudSessionCredential session = awsSessionCredentialMapper.toCloudSessionCredential(
                response, longTermCredentials.getRegion());
        
        return session;
    } finally {
        stsClient.close();
    }
}
```

### 세션 캐싱 전략

- **캐시 키**: `tenantKey:accountScope:providerType`
- **TTL 계산**: 세션 만료 시간에서 5분을 뺀 값 (안전 마진)
- **캐시 저장소**: Redis (SessionCacheService에서 관리)

### CloudSessionCredential 인터페이스

```java
public interface CloudSessionCredential {
    ProviderType getProviderType();
    LocalDateTime getExpiresAt();
    
    default boolean isValid() {
        return getExpiresAt() != null && getExpiresAt().isAfter(LocalDateTime.now());
    }
    
    default boolean isExpiringSoon(int bufferMinutes) {
        if (getExpiresAt() == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(bufferMinutes).isAfter(getExpiresAt());
    }
}
```

### 주요 포인트

1. **JIT 방식**: 작업 수행 시점에 필요한 세션을 동적으로 획득합니다.
2. **캐싱**: 세션을 Redis에 캐싱하여 반복 요청 시 성능을 향상시킵니다.
3. **자동 갱신**: 캐시된 세션이 만료되었으면 자동으로 새 세션을 발급합니다.
4. **보안**: 장기 자격증명은 메모리에만 존재하며, 세션 토큰만 전달됩니다.

---

## CloudResourceUseCase에서 자격증명 해결 방법

### 패턴 1: 단기 세션 사용 (권장)

리소스 작업(생성, 수정, 삭제 등) 시 단기 세션을 사용합니다.

```java
@Service
@RequiredArgsConstructor
public class CloudResourceUseCaseService {
    
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    private final ResourcePortRouter router;
    
    @Transactional
    public void start(ResourceIdentity id, String serviceKey, String resourceType) {
        // 1. JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, id.getAccountScope(), id.getProviderType());
        
        // 2. 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).start(id, session);
    }
    
    @Transactional
    public void stop(ResourceIdentity id, String serviceKey, String resourceType) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, id.getAccountScope(), id.getProviderType());
        
        router.lifecycle(id.getProviderType()).stop(id, session);
    }
}
```

**사용 시나리오:**
- 리소스 생성/수정/삭제
- 리소스 시작/중지/종료
- 리소스 설정 변경

### 패턴 2: 장기 자격증명 직접 해결 (비권장)

특수한 경우에만 장기 자격증명을 직접 해결합니다. 일반적으로는 사용하지 않습니다.

```java
@Service
@RequiredArgsConstructor
public class ObjectStorageUseCaseService {
    
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    
    private void setupTenantContextAndCredentials(
            CloudProvider.ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKey();
        // 장기 자격증명 해결 (내부적으로 암호화 해제)
        accountCredentialManagementPort.resolveCredentials(
                tenantKey, providerType, accountScope);
    }
    
    @Transactional
    public CloudResource createContainer(
            CloudProvider.ProviderType providerType, 
            CreateObjectStorageContainerRequest request) {
        
        // 자격증명 해결
        setupTenantContextAndCredentials(providerType, request.getRegion());
        
        // 리소스 작업 수행
        CreateObjectStorageContainerCommand command = ...;
        return router.management(providerType).createContainer(command);
    }
}
```

**주의사항:**
- 이 패턴은 레거시 코드에서 사용되며, 신규 개발 시에는 패턴 1을 사용해야 합니다.
- 장기 자격증명은 보안상 위험하므로 가능한 한 단기 세션을 사용해야 합니다.

### 패턴 3: Discovery 작업에서의 자격증명 해결

리소스 조회(Discovery) 작업에서는 Adapter 내부에서 단기 세션을 획득합니다. 


```java
// Adapter 내부
/**
 * 세션 자격증명을 획득합니다 (패턴 3: Discovery 작업에서의 자격증명 해결).
 * AccountCredentialManagementPort를 통해 테넌트별 단기 세션을 획득합니다.
 */
private CloudSessionCredential getSession() {
    try {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        String accountScope = getAccountScopeFromContext();
        CloudProvider.ProviderType providerType = CloudProvider.ProviderType.AWS;

        log.debug("세션 획득 시작: tenantKey={}, providerType={}, accountScope={}", 
                tenantKey, providerType, accountScope);

        // AccountCredentialManagementPort를 통해 단기 세션 획득
        CloudSessionCredential session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, providerType);

        if (session == null) {
            log.error("세션 획득 결과가 null입니다: tenantKey={}, accountScope={}", tenantKey, accountScope);
            throw new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID, 
                    "세션 획득에 실패했습니다: 세션이 null입니다");
        }

        log.debug("세션 획득 완료: expiresAt={}", session.getExpiresAt());
        return session;

    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        log.error("세션 획득 실패: {}", e.getMessage(), e);
        throw new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID, 
                "세션 획득에 실패했습니다: " + e.getMessage());
    }
}

// 세션을 사용하여 AWS Client 생성
private S3Client createS3Client(CloudSessionCredential session, String region) {
    if (!(session instanceof AwsSessionCredential awsSession)) {
        throw new IllegalArgumentException("AWS 세션이 필요합니다: " + session.getClass().getSimpleName());
    }
    
    // AwsSessionCredential을 사용하여 S3Client 생성
    // ...
}
```

**주요 포인트:**
- Discovery 작업에서도 단기 세션(`getSession`)을 사용합니다.
- Adapter 내부에서 `getSession()`을 호출하여 세션을 획득합니다.
- 획득한 세션을 사용하여 AWS SDK Client를 동적으로 생성합니다.
- 작업 완료 후 Client를 `close()`하여 리소스를 정리합니다.

**참고:**
- 레거시 코드에서는 `resolveCredentials()`를 사용할 수 있으나, 신규 개발 시에는 `getSession()`을 사용해야 합니다.
- `resolveCredentials()`는 장기 자격증명을 반환하므로 보안상 위험할 수 있습니다.

### AccountCredentialPortRouter 사용

프로바이더별로 적절한 Adapter를 자동으로 라우팅합니다.

```java
@Component
@Primary
public class AccountCredentialPortRouter implements AccountCredentialManagementPort {
    
    private final Map<ProviderType, AccountCredentialManagementPort> credentialPorts;
    
    @Override
    public CloudSessionCredential getSession(
            String tenantKey, String accountScope, ProviderType providerType) {
        // 프로바이더 타입에 따라 적절한 Adapter 선택
        return credential(providerType).getSession(tenantKey, accountScope, providerType);
    }
}
```

**사용 방법:**
- UseCase Service에서는 `AccountCredentialManagementPort`를 직접 주입받아 사용
- Spring이 자동으로 `AccountCredentialPortRouter`를 주입 (Primary 어노테이션)
- 프로바이더별 Adapter는 자동으로 선택됨

---

## 모범 사례

### 1. 항상 단기 세션 사용

```java
// ✅ 권장
CloudSessionCredential session = accountCredentialManagementPort.getSession(
        tenantKey, accountScope, providerType);
router.lifecycle(providerType).start(id, session);

// ❌ 비권장
Object credentials = accountCredentialManagementPort.resolveCredentials(
        tenantKey, providerType, accountScope);
// 장기 자격증명 직접 사용
```

### 2. TenantContextHolder 사용

```java
// ✅ 권장
String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

// ❌ 비권장
String tenantKey = request.getTenantKey(); // 요청에서 직접 추출
```

### 3. AccountScope 확인

```java
// ✅ 권장
String accountScope = id.getAccountScope();
if (accountScope == null) {
    throw new BusinessException(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED, 
                               "AccountScope가 필요합니다");
}

// ❌ 비권장
String accountScope = "default"; // 하드코딩
```

### 4. 에러 처리

```java
// ✅ 권장
try {
    CloudSessionCredential session = accountCredentialManagementPort.getSession(
            tenantKey, accountScope, providerType);
    router.lifecycle(providerType).start(id, session);
} catch (BusinessException e) {
    if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
        log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                 tenantKey, accountScope);
        throw new BusinessException(CloudErrorCode.ACCOUNT_NOT_CONFIGURED, 
                                   "계정이 설정되지 않았습니다");
    }
    throw e;
}

// ❌ 비권장
CloudSessionCredential session = accountCredentialManagementPort.getSession(
        tenantKey, accountScope, providerType); // 예외 처리 없음
```

### 5. 로깅

```java
// ✅ 권장
log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}", 
         tenantKey, accountScope, providerType);
CloudSessionCredential session = accountCredentialManagementPort.getSession(
        tenantKey, accountScope, providerType);
log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());

// ❌ 비권장
CloudSessionCredential session = accountCredentialManagementPort.getSession(
        tenantKey, accountScope, providerType); // 로깅 없음
```

