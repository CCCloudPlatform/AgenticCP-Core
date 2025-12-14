# 멀티 클라우드 헥사고날 아키텍처 개발 가이드

## 📋 개요

AgenticCP의 멀티 클라우드 리소스 통합을 위한 헥사고날 아키텍처(Ports & Adapters) 구현 가이드입니다. 이 문서는 개발자들이 헥사고날 아키텍처의 핵심 개념을 이해하고, 실제 개발 작업을 진행할 수 있도록 단계별로 안내합니다.

## 🎯 왜 헥사고날 아키텍처인가?

### 기존 문제점
- CSP별 API 차이로 인한 복잡한 조건문 분기
- 새로운 CSP 추가 시 기존 코드 수정 필요
- 외부 의존성과 비즈니스 로직의 강결합
- 테스트 어려움 (실제 CSP API 호출 필요)

### 헥사고날 아키텍처의 해결책
- **포트(Port)**: 비즈니스 로직과 외부 시스템 간의 계약 정의
- **어댑터(Adapter)**: 외부 시스템과의 실제 통신 구현
- **도메인 중심**: 비즈니스 로직이 외부 의존성에 영향받지 않음
- **테스트 용이성**: 포트 인터페이스만으로 테스트 가능

## 🏗️ 아키텍처 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    멀티 클라우드 플랫폼                      │
├─────────────────────────────────────────────────────────────┤
│  Controller (REST)  │  UseCase Service  │  Domain Entity    │
│  (Inbound Adapter)  │  (Application)    │  (Business Logic) │
├─────────────────────────────────────────────────────────────┤
│                    Ports (Interfaces)                       │
│  ResourceDiscovery  │  ResourceLifecycle │  ResourceTagging  │
├─────────────────────────────────────────────────────────────┤
│                    Adapters (Implementations)               │
│  AWS Adapter       │  Azure Adapter     │  GCP Adapter      │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 핵심 구성 요소

### 1. 포트(Ports) - 비즈니스 계약

포트는 도메인과 외부 시스템 간의 **계약(Contract)**을 정의합니다.

#### 🔍 계약(Contract)이란?

**계약**은 두 시스템 간의 상호작용 규칙을 명시한 문서입니다. 소프트웨어에서 계약은:
- **인터페이스**: 어떤 메서드를 제공하는가?
- **입력 규격**: 어떤 파라미터를 받는가?
- **출력 규격**: 어떤 결과를 반환하는가?
- **사이드 이펙트**: 어떤 부작용이 있는가?
- **예외 상황**: 어떤 오류가 발생할 수 있는가?

#### 🎯 계약의 목표

```java
// 예시: 리소스 발견 포트 계약
public interface ResourceDiscoveryPort {
    /**
     * 리소스 목록을 조회합니다.
     * 
     * @param query 조회 조건 (페이징, 필터링 포함)
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     * @throws BusinessException 조회 권한 없음, 잘못된 쿼리 조건
     */
    Page<CloudResource> listResources(ResourceQuery query);
    
    /**
     * 특정 리소스를 조회합니다.
     * 
     * @param id 리소스 식별자 (null 불가)
     * @return CloudResource (존재하지 않으면 Optional.empty())
     * @throws BusinessException 잘못된 식별자 형식
     */
    Optional<CloudResource> getResource(ResourceIdentity id);
}
```

**계약의 핵심 목표:**
1. **명확한 의사소통**: 개발자들이 "무엇을 해야 하는지" 명확히 이해
2. **변경 영향 최소화**: 계약이 변경되지 않는 한 구현체 변경이 다른 코드에 영향 없음
3. **테스트 용이성**: 계약만으로도 테스트 케이스 작성 가능
4. **다중 구현 지원**: 하나의 계약에 여러 구현체 제공 가능

#### 💡 계약이 필요한 이유

**문제 상황:**
```java
// ❌ 계약 없는 직접 호출 방식
@Service
public class CloudResourceService {
    
    public List<CloudResource> getAwsResources() {
        // AWS SDK 직접 호출
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);
        
        List<CloudResource> resources = new ArrayList<>();
        for (Instance instance : response.reservations().get(0).instances()) {
            CloudResource resource = CloudResource.builder()
                .resourceId(instance.instanceId())
                .resourceName(getInstanceName(instance))
                .instanceType(instance.instanceType())
                .build();
            resources.add(resource);
        }
        return resources;
    }
    
    public List<CloudResource> getAzureResources() {
        // Azure SDK 직접 호출 - 완전히 다른 방식!
        PagedIterable<VirtualMachine> vms = computeClient.virtualMachines().list();
        
        List<CloudResource> resources = new ArrayList<>();
        for (VirtualMachine vm : vms) {
            CloudResource resource = CloudResource.builder()
                .resourceId(vm.name())
                .resourceName(vm.name())
                .instanceType(vm.size())
                .build();
            resources.add(resource);
        }
        return resources;
    }
}
```

**문제점:**
- AWS와 Azure 코드가 완전히 다름
- 새로운 CSP 추가 시 기존 코드 대폭 수정
- 테스트 시 실제 CSP API 호출 필요
- 에러 처리 방식이 CSP별로 다름

**계약 도입 후:**
```java
// ✅ 계약 기반 방식
@Service
public class CloudResourceService {
    
    private final ResourcePortRouter router;
    
    public Page<CloudResource> listResources(ResourceQuery query) {
        // 계약을 통한 일관된 호출
        return router.discovery(query.getProviderType())
                   .listResources(query);
    }
}

// AWS 구현체
@Component
public class AwsResourceDiscoveryAdapter implements ResourceDiscoveryPort {
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        // AWS 특화 로직
        DescribeInstancesRequest request = buildAwsRequest(query);
        DescribeInstancesResponse response = ec2Client.describeInstances(request);
        return awsMapper.toCloudResources(response);
    }
}

// Azure 구현체  
@Component
public class AzureResourceDiscoveryAdapter implements ResourceDiscoveryPort {
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        // Azure 특화 로직
        PagedIterable<VirtualMachine> vms = computeClient.virtualMachines().list();
        return azureMapper.toCloudResources(vms);
    }
}
```

**장점:**
- 서비스 코드는 CSP에 무관하게 동일
- 새로운 CSP 추가 시 어댑터만 추가
- 테스트 시 Mock 구현체 사용 가능
- 에러 처리가 표준화됨

#### 📋 계약 설계 원칙

**1. 도메인 중심 설계**
```java
// ✅ 좋은 예: 비즈니스 용어 사용
public interface ResourceDiscoveryPort {
    Page<CloudResource> listResources(ResourceQuery query);
    Optional<CloudResource> getResource(ResourceIdentity id);
}

// ❌ 나쁜 예: 기술 용어 사용
public interface Ec2ApiPort {
    DescribeInstancesResponse describeInstances(DescribeInstancesRequest request);
}
```

**2. 외부 기술 독립성**
```java
// ✅ 좋은 예: 기술 중립적
public interface ResourceLifecyclePort {
    void start(ResourceIdentity id);
    void stop(ResourceIdentity id);
}

// ❌ 나쁜 예: AWS 특화
public interface Ec2LifecyclePort {
    void startInstances(StartInstancesRequest request);
    void stopInstances(StopInstancesRequest request);
}
```

**3. 명확한 예외 계약**
```java
// ✅ 좋은 예: 명확한 예외 정의
public interface ResourceTaggingPort {
    /**
     * 리소스에 태그를 설정합니다.
     * 
     * @param id 리소스 식별자
     * @param tags 태그 맵 (null 불가, 빈 맵 가능)
     * @throws BusinessException 리소스 없음, 권한 없음, 잘못된 태그 형식
     */
    void putTags(ResourceIdentity id, Map<String, String> tags);
}
```

**4. 일관된 반환 타입**
```java
// ✅ 좋은 예: 일관된 반환 타입
public interface ResourceCostPort {
    CostInfo getCost(ResourceIdentity id);           // 단일 객체
    Page<CostInfo> listCosts(CostQuery query);      // 페이지네이션
    Optional<CostInfo> getCostIfAvailable(ResourceIdentity id); // 선택적 반환
}
```

#### 🧪 계약 테스트의 중요성

**계약 테스트**는 모든 구현체가 계약을 올바르게 준수하는지 검증합니다.

```java
// 계약 테스트 예시
public abstract class ResourceDiscoveryContractTest {
    
    protected abstract ResourceDiscoveryPort port();
    
    @Test
    void listResources_shouldReturnPage_whenValidQuery() {
        // Given
        ResourceQuery query = ResourceQuery.builder()
            .page(0)
            .size(10)
            .build();
        
        // When
        Page<CloudResource> result = port().listResources(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getSize()).isLessThanOrEqualTo(10);
    }
    
    @Test
    void getResource_shouldReturnEmpty_whenResourceNotFound() {
        // Given
        ResourceIdentity id = ResourceIdentity.builder()
            .providerResourceId("non-existent-id")
            .build();
        
        // When
        Optional<CloudResource> result = port().getResource(id);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void getResource_shouldThrowException_whenInvalidId() {
        // Given
        ResourceIdentity invalidId = ResourceIdentity.builder()
            .providerResourceId("")  // 빈 문자열
            .build();
        
        // When & Then
        assertThatThrownBy(() -> port().getResource(invalidId))
            .isInstanceOf(BusinessException.class);
    }
}

// AWS 구현체 테스트
class AwsResourceDiscoveryAdapterContractTest extends ResourceDiscoveryContractTest {
    
    @Override
    protected ResourceDiscoveryPort port() {
        return new AwsResourceDiscoveryAdapter(mockEc2Client, mockMapper);
    }
}

// Azure 구현체 테스트
class AzureResourceDiscoveryAdapterContractTest extends ResourceDiscoveryContractTest {
    
    @Override
    protected ResourceDiscoveryPort port() {
        return new AzureResourceDiscoveryAdapter(mockComputeClient, mockMapper);
    }
}
```

**계약 테스트의 장점:**
- 모든 구현체가 동일한 동작 보장
- 새로운 구현체 추가 시 기존 테스트 재사용
- 계약 변경 시 모든 구현체에 영향 확인
- 문서화 효과 (계약의 올바른 사용법 예시)

#### 🏢 비즈니스 가치와 운영 이점

**1. 개발 생산성 향상**
```java
// 계약이 있으면 새로운 CSP 추가가 간단
@Component
public class GcpResourceDiscoveryAdapter implements ResourceDiscoveryPort {
    
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        // GCP 특화 로직만 구현하면 됨
        List<Instance> instances = computeService.instances().list(query.getAccountScope()).execute().getItems();
        return gcpMapper.toCloudResources(instances);
    }
}

// 기존 서비스 코드는 전혀 변경할 필요 없음!
@Service
public class CloudResourceService {
    public Page<CloudResource> listResources(ResourceQuery query) {
        return router.discovery(query.getProviderType()).listResources(query);
        // ↑ 이 코드는 AWS, Azure, GCP 모두 동일하게 동작
    }
}
```

**2. 운영 안정성 확보**
```java
// 계약을 통한 일관된 에러 처리
public class CloudResourceService {
    
    public Page<CloudResource> listResources(ResourceQuery query) {
        try {
            return router.discovery(query.getProviderType()).listResources(query);
        } catch (BusinessException e) {
            // 모든 CSP에서 동일한 에러 처리
            auditLogger.log("RESOURCE_LIST_FAILED", e.getErrorCode().getCode());
            throw e;
        }
    }
}

// 각 어댑터는 CSP별 에러를 표준 에러로 변환
@Component
public class AwsResourceDiscoveryAdapter implements ResourceDiscoveryPort {
    
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        try {
            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            return mapper.toCloudResources(response);
        } catch (AmazonServiceException e) {
            // AWS 특화 에러를 표준 에러로 변환
            throw CloudErrorTranslator.translate(e);
        }
    }
}
```

**3. 테스트 및 품질 보장**
```java
// 실제 CSP 없이도 테스트 가능
@Test
void shouldListResources_whenValidQuery() {
    // Given
    ResourceDiscoveryPort mockPort = mock(ResourceDiscoveryPort.class);
    when(mockPort.listResources(any())).thenReturn(mockPage);
    
    CloudResourceService service = new CloudResourceService(mockPort);
    
    // When
    Page<CloudResource> result = service.listResources(query);
    
    // Then
    assertThat(result).isNotNull();
    verify(mockPort).listResources(query);
}
```

**4. 점진적 마이그레이션 지원**
```java
// 기존 코드를 점진적으로 마이그레이션
@Service
public class LegacyCloudResourceService {
    
    // 기존 방식 (점진적으로 제거)
    @Deprecated
    public List<CloudResource> getAwsResourcesLegacy() {
        // 기존 AWS 직접 호출 코드
    }
    
    // 새로운 방식 (계약 기반)
    public Page<CloudResource> listResources(ResourceQuery query) {
        return router.discovery(query.getProviderType()).listResources(query);
    }
}
```

**5. 모니터링 및 관찰성**
```java
// 계약을 통한 일관된 메트릭 수집
@Component
public class CloudResourceService {
    
    private final MeterRegistry meterRegistry;
    
    public Page<CloudResource> listResources(ResourceQuery query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Page<CloudResource> result = router.discovery(query.getProviderType())
                                             .listResources(query);
            
            // 성공 메트릭
            meterRegistry.counter("cloud.resource.list.success", 
                                "provider", query.getProviderType().name())
                        .increment();
            
            return result;
        } catch (Exception e) {
            // 실패 메트릭
            meterRegistry.counter("cloud.resource.list.failure", 
                                "provider", query.getProviderType().name(),
                                "error", e.getClass().getSimpleName())
                        .increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("cloud.resource.list.duration")
                           .tag("provider", query.getProviderType().name())
                           .register(meterRegistry));
        }
    }
}
```

#### 🎯 계약 설계 시 고려사항

**1. 버전 관리**
```java
// 계약 변경 시 하위 호환성 고려
public interface ResourceDiscoveryPort {
    
    // 기존 메서드 (유지)
    Page<CloudResource> listResources(ResourceQuery query);
    
    // 새로운 메서드 (기본 구현 제공)
    default Page<CloudResource> listResourcesWithFilter(ResourceQuery query, 
                                                        ResourceFilter filter) {
        // 기본 구현: 필터 무시하고 기존 메서드 호출
        return listResources(query);
    }
}
```

**2. 성능 고려**
```java
// 대용량 데이터 처리를 위한 스트리밍 계약
public interface ResourceDiscoveryPort {
    
    // 일반적인 페이징
    Page<CloudResource> listResources(ResourceQuery query);
    
    // 대용량 데이터용 스트리밍
    Stream<CloudResource> streamResources(ResourceQuery query);
    
    // 배치 처리용
    CompletableFuture<Page<CloudResource>> listResourcesAsync(ResourceQuery query);
}
```

**3. 보안 고려**
```java
public interface ResourceDiscoveryPort {
    
    /**
     * 리소스 목록을 조회합니다.
     * 
     * @param query 조회 조건
     * @param context 보안 컨텍스트 (권한, 테넌트 정보)
     * @return 권한이 있는 리소스만 반환
     */
    Page<CloudResource> listResources(ResourceQuery query, SecurityContext context);
}
```

**포트의 특징:**
- 도메인 중심의 인터페이스
- 외부 기술에 의존하지 않음
- 비즈니스 요구사항을 반영
- 테스트 가능한 계약
- 확장 가능한 설계
- 운영 친화적 구조

### 2. 어댑터(Adapters) - 외부 시스템 연동

어댑터는 포트를 구현하여 실제 외부 시스템과 통신합니다.

```java
// 예시: AWS 어댑터
@Component
public class AwsResourceDiscoveryAdapter implements ResourceDiscoveryPort, ProviderScoped {
    
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        try {
            // AWS SDK 호출
            DescribeInstancesRequest request = buildRequest(query);
            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            return mapper.toCloudResources(response);
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }
}
```

**어댑터의 특징:**
- 포트 인터페이스 구현
- 외부 시스템별 특화된 로직
- 에러 변환 및 매핑 담당
- ProviderScoped 인터페이스 구현

### 3. 라우터(Router) - 전략 패턴

라우터는 요청에 따라 적절한 어댑터를 선택합니다.

```java
@Component
public class ResourcePortRouter {
    
    public ResourceDiscoveryPort discovery(ProviderType type) {
        return require(discoveryMap, type);
    }
    
    public ResourceLifecyclePort lifecycle(ProviderType type) {
        return require(lifecycleMap, type);
    }
}
```

## 🚀 개발 진행 순서

### Phase 1: 포트 정의 및 모델 설계 (1-2일)

#### 1.1 도메인 모델 정의
```java
// ResourceIdentity - 리소스 식별자
@Value
@Builder
public class ResourceIdentity {
    ProviderType providerType;
    String accountScope;    // AWS: AccountId, Azure: SubscriptionId, GCP: ProjectId
    String region;
    String providerResourceId;
    String serviceKey;      // EC2, Compute, VM
    String resourceType;
}

// ResourceQuery - 조회 조건
@Value
@Builder
public class ResourceQuery {
    ProviderType providerType;
    String accountScope;
    Set<String> regions;
    String nameContains;
    String resourceType;
    Map<String, String> tagsEquals;
    Instant changedAfter;
    int page;
    int size;
}
```

#### 1.2 포트 인터페이스 정의
```java
// 리소스 발견 포트
public interface ResourceDiscoveryPort {
    Page<CloudResource> listResources(ResourceQuery query);
    Optional<CloudResource> getResource(ResourceIdentity id);
}

// 리소스 생명주기 포트
public interface ResourceLifecyclePort {
    void start(ResourceIdentity id);
    void stop(ResourceIdentity id);
    void terminate(ResourceIdentity id);
}

// 리소스 태깅 포트
public interface ResourceTaggingPort {
    void putTags(ResourceIdentity id, Map<String, String> tags);
    Map<String, String> getTags(ResourceIdentity id);
}
```

**개발 팁:**
- 포트는 비즈니스 요구사항을 반영해야 함
- 외부 기술 스펙에 의존하지 말 것
- 메서드명은 도메인 용어 사용

### Phase 2: Capability 시스템 구현 (1일)

#### 2.1 Capability 모델
```java
@Value
@Builder
public class CspCapability {
    boolean supportsStart;
    boolean supportsStop;
    boolean supportsTerminate;
    boolean supportsTagging;
    boolean supportsListByTag;
}
```

#### 2.2 Capability Registry
```java
@Component
public class CapabilityRegistry {
    
    public void register(ProviderType providerType, String serviceKey, 
                        String resourceType, CspCapability capability) {
        registry.put(key(providerType, serviceKey, resourceType), capability);
    }
    
    public CspCapability get(ProviderType providerType, String serviceKey, 
                            String resourceType) {
        return registry.get(key(providerType, serviceKey, resourceType));
    }
}
```

#### 2.3 Capability Guard
```java
@Component
public class CapabilityGuard {
    
    public void ensureSupported(ProviderType providerType, String serviceKey, 
                               String resourceType, Operation op) {
        CspCapability cap = capabilityRegistry.get(providerType, serviceKey, resourceType);
        if (cap == null) {
            throw new BusinessException(CloudErrorCode.CAPABILITY_NOT_DEFINED);
        }
        
        switch (op) {
            case START -> {
                if (!cap.isSupportsStart()) 
                    throw new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION);
            }
            // ... 다른 작업들
        }
    }
}
```

**개발 팁:**
- CSP별 지원 기능을 런타임에 검증
- 미지원 작업 시 명확한 에러 메시지 제공
- 새로운 CSP 추가 시 Capability만 등록하면 됨

### Phase 3: 라우터 및 유스케이스 서비스 (1일)

#### 3.1 ResourcePortRouter
```java
@Component
public class ResourcePortRouter {
    
    private final Map<ProviderType, ResourceDiscoveryPort> discovery = new EnumMap<>(ProviderType.class);
    private final Map<ProviderType, ResourceLifecyclePort> lifecycle = new EnumMap<>(ProviderType.class);
    
    public ResourcePortRouter(List<ResourceDiscoveryPort> discoveryPorts,
                              List<ResourceLifecyclePort> lifecyclePorts) {
        // Spring이 자동으로 어댑터들을 주입
        discoveryPorts.stream()
            .filter(p -> p instanceof ProviderScoped)
            .forEach(p -> discovery.put(((ProviderScoped) p).getProviderType(), p));
    }
    
    public ResourceDiscoveryPort discovery(ProviderType type) {
        return require(discovery, type);
    }
}
```

#### 3.2 유스케이스 서비스
```java
@Service
@RequiredArgsConstructor
public class CloudResourceUseCaseService {
    
    private final ResourcePortRouter router;
    private final CapabilityGuard capabilityGuard;
    private final AuditEventPort auditEventPort;
    
    public Page<CloudResource> list(ResourceQuery query) {
        try (AutoCloseable span = tracingPort.startSpan("cloud.listResources", 
                Map.of("provider", query.getProviderType().name()))) {
            
            Page<CloudResource> page = router.discovery(query.getProviderType())
                .listResources(query);
            
            auditEventPort.record("LIST_RESOURCES", "CloudResource", "SUCCESS",
                Map.of("provider", query.getProviderType().name(), 
                       "count", page.getTotalElements()));
            
            return page;
        } catch (RuntimeException ex) {
            auditEventPort.record("LIST_RESOURCES", "CloudResource", "FAILURE",
                Map.of("provider", query.getProviderType().name(), 
                       "error", ex.getMessage()));
            throw ex;
        }
    }
    
    @Transactional
    public void start(ResourceIdentity id, String serviceKey, String resourceType) {
        // 1. Capability 검증
        capabilityGuard.ensureSupported(id.getProviderType(), serviceKey, 
                                       resourceType, CapabilityGuard.Operation.START);
        
        // 2. 자격증명 확인
        credentialProviderPort.resolveCredentials(null, id.getProviderType(), 
                                                 id.getAccountScope());
        
        // 3. 실제 작업 수행
        router.lifecycle(id.getProviderType()).start(id);
        
        // 4. 감사 로그
        auditEventPort.record("START", "CloudResource", "SUCCESS", 
                            Map.of("id", id.getProviderResourceId()));
    }
}
```

**개발 팁:**
- 유스케이스는 포트만 의존해야 함
- 트랜잭션, 감사, 추적은 공통 처리
- 비즈니스 규칙은 도메인 서비스에 위임

### Phase 4: 공통 어댑터 구현 (1일)

#### 4.1 에러 변환기
```java
public final class CloudErrorTranslator {
    
    public static RuntimeException translate(Throwable t) {
        String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        
        if (msg.contains("rate") && msg.contains("limit")) {
            return new BusinessException(CloudErrorCode.API_RATE_LIMIT_EXCEEDED);
        }
        if (msg.contains("timeout")) {
            return new BusinessException(CloudErrorCode.API_TIMEOUT);
        }
        
        return new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
    }
}
```

#### 4.2 회복탄력성 실행기
```java
public class ResilientPortExecutor {
    
    public static <T> T execute(Supplier<T> supplier) {
        // TODO: Resilience4j 의존성 추가 후 실제 구현
        return supplier.get();
    }
}
```

#### 4.3 ProviderScoped 인터페이스
```java
public interface ProviderScoped {
    ProviderType getProviderType();
}
```

**개발 팁:**
- CSP별 에러를 도메인 에러로 변환
- 공통 회복탄력성 정책 적용
- 어댑터 식별을 위한 인터페이스 제공

### Phase 5: AWS 어댑터 구현 (2-3일)

#### 5.1 AWS 매퍼
```java
@Component
public class AwsResourceMapper {
    
    public CloudResource toCloudResource(Instance awsInstance) {
        return CloudResource.builder()
            .resourceId(awsInstance.getInstanceId())
            .resourceName(getInstanceName(awsInstance))
            .displayName(getInstanceName(awsInstance))
            .lifecycleState(mapLifecycleState(awsInstance.getState().getName()))
            .instanceType(awsInstance.getInstanceType())
            .cpuCores(getCpuCores(awsInstance.getInstanceType()))
            .memoryGb(getMemoryGb(awsInstance.getInstanceType()))
            .publicIpAddress(awsInstance.getPublicIpAddress())
            .privateIpAddress(awsInstance.getPrivateIpAddress())
            .tags(mapTags(awsInstance.getTags()))
            .configuration(JsonUtils.toJson(awsInstance)) // 원본 보존
            .metadata(buildMetadata(awsInstance))
            .build();
    }
    
    private LifecycleState mapLifecycleState(String awsState) {
        return switch (awsState) {
            case "running" -> LifecycleState.RUNNING;
            case "stopped" -> LifecycleState.STOPPED;
            case "stopping" -> LifecycleState.STOPPING;
            case "pending" -> LifecycleState.PENDING;
            case "terminated" -> LifecycleState.TERMINATED;
            case "terminating" -> LifecycleState.TERMINATING;
            default -> LifecycleState.UNKNOWN;
        };
    }
}
```

#### 5.2 AWS Discovery 어댑터
```java
@Component
@RequiredArgsConstructor
public class AwsResourceDiscoveryAdapter implements ResourceDiscoveryPort, ProviderScoped {
    
    private final Ec2Client ec2Client;
    private final AwsResourceMapper mapper;
    
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        try {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(buildFilters(query))
                .build();
            
            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            
            List<CloudResource> resources = response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .map(mapper::toCloudResource)
                .collect(Collectors.toList());
            
            return new PageImpl<>(resources);
            
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }
    
    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
```

#### 5.3 AWS Lifecycle 어댑터
```java
@Component
@RequiredArgsConstructor
public class AwsResourceLifecycleAdapter implements ResourceLifecyclePort, ProviderScoped {
    
    private final Ec2Client ec2Client;
    
    @Override
    public void start(ResourceIdentity id) {
        try {
            StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(id.getProviderResourceId())
                .build();
            
            ec2Client.startInstances(request);
            
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }
    
    @Override
    public void stop(ResourceIdentity id) {
        try {
            StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(id.getProviderResourceId())
                .build();
            
            ec2Client.stopInstances(request);
            
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }
    
    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
```

**개발 팁:**
- Canonical 모델로 매핑 시 필수 필드 우선 처리
- CSP별 특수 필드는 metadata에 보존
- 에러는 반드시 CloudErrorTranslator로 변환

### Phase 6: 테스트 구현 (1-2일)

#### 6.1 계약 테스트
```java
public abstract class ResourceDiscoveryContractTest {
    
    protected abstract ResourceDiscoveryPort port();
    
    @Test
    void listResources_shouldNotThrow_andReturnPage() {
        ResourceQuery query = ResourceQuery.builder()
            .page(0)
            .size(10)
            .build();
            
        Page<CloudResource> page = port().listResources(query);
        
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotNull();
    }
    
    @Test
    void getResource_shouldReturnEmpty_whenResourceNotFound() {
        ResourceIdentity id = ResourceIdentity.builder()
            .providerResourceId("non-existent-id")
            .build();
            
        Optional<CloudResource> result = port().getResource(id);
        
        assertThat(result).isEmpty();
    }
}
```

#### 6.2 AWS 통합 테스트
```java
@EnabledIfEnvironmentVariable(named = "ENABLE_LOCALSTACK", matches = "true")
class AwsResourceDiscoveryAdapterIT extends ResourceDiscoveryContractTest {
    
    @Testcontainers
    static class LocalStackContainer {
        @Container
        static GenericContainer<?> localstack = new GenericContainer<>("localstack/localstack")
            .withEnv("SERVICES", "ec2")
            .withExposedPorts(4566);
    }
    
    @Override
    protected ResourceDiscoveryPort port() {
        // LocalStack을 사용한 실제 AWS SDK 테스트
        return awsResourceDiscoveryAdapter;
    }
}
```

**개발 팁:**
- 계약 테스트로 포트 인터페이스 검증
- LocalStack/Azurite로 실제 CSP API 테스트
- 매퍼 스냅샷 테스트로 데이터 정합성 보장

## 📚 핵심 개발 원칙

### 1. Canonical 모델 (반부패 계층)
- **목적**: CSP별 차이를 도메인 모델에서 흡수
- **구현**: CloudResource 엔티티를 Canonical로 사용
- **매핑 규칙**: 
  - 필수 필드는 정규화하여 매핑
  - 비정형 데이터는 configuration/metadata에 보존
  - 단위 변환 (vCPU → cpuCores, MiB → memoryGb)

### 2. Capability 기반 검증
- **목적**: 런타임에 CSP별 지원 기능 검증
- **구현**: CapabilityRegistry + CapabilityGuard
- **장점**: 새로운 CSP 추가 시 코드 변경 최소화

### 3. 에러 변환 표준화
- **목적**: CSP별 에러를 도메인 에러로 통일
- **구현**: CloudErrorTranslator
- **장점**: 일관된 에러 처리 및 사용자 경험

### 4. 회복탄력성 표준화
- **목적**: CSP API 호출의 안정성 확보
- **구현**: Resilience4j + ResilientPortExecutor
- **정책**: 서킷브레이커, 재시도, 레이트리미터, 벌크헤드

### 5. 감사/추적 표준화
- **목적**: 모든 CSP 연동에 대한 일관된 모니터링
- **구현**: AuditEventPort + TracingPort
- **장점**: 운영 가시성 및 디버깅 용이성

## 🔄 새로운 CSP 추가 방법

### 1단계: Capability 등록
```java
@PostConstruct
public void initializeCapabilities() {
    // Azure EC2 지원 기능 등록
    capabilityRegistry.register(
        ProviderType.AZURE, 
        "COMPUTE", 
        "INSTANCE",
        CspCapability.builder()
            .supportsStart(true)
            .supportsStop(true)
            .supportsTerminate(true)
            .supportsTagging(true)
            .build()
    );
}
```

### 2단계: 어댑터 구현
```java
@Component
public class AzureResourceDiscoveryAdapter implements ResourceDiscoveryPort, ProviderScoped {
    
    private final ComputeManagementClient computeClient;
    private final AzureResourceMapper mapper;
    
    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        // Azure SDK 호출 로직
    }
    
    @Override
    public ProviderType getProviderType() {
        return ProviderType.AZURE;
    }
}
```

### 3단계: 매퍼 구현
```java
@Component
public class AzureResourceMapper {
    
    public CloudResource toCloudResource(VirtualMachine azureVm) {
        return CloudResource.builder()
            .resourceId(azureVm.name())
            .resourceName(azureVm.name())
            .lifecycleState(mapLifecycleState(azureVm.provisioningState()))
            .instanceType(azureVm.size())
            .cpuCores(getCpuCores(azureVm.size()))
            .memoryGb(getMemoryGb(azureVm.size()))
            .configuration(JsonUtils.toJson(azureVm))
            .build();
    }
}
```

**핵심**: 기존 코드 변경 없이 새로운 어댑터만 추가하면 됨!

## 🧪 테스트 전략

### 1. 계약 테스트 (Contract Tests)
- **목적**: 포트 인터페이스의 계약 검증
- **범위**: 모든 포트 인터페이스
- **실행**: 모든 어댑터 구현체

### 2. 통합 테스트 (Integration Tests)
- **목적**: 실제 CSP API와의 연동 검증
- **도구**: LocalStack(AWS), Azurite(Azure), GCP Emulator
- **실행**: 환경 변수로 제어

### 3. 매퍼 테스트 (Mapper Tests)
- **목적**: CSP 응답 → Canonical 변환 정확성
- **방법**: 스냅샷 테스트 또는 JSON 비교
- **데이터**: 실제 CSP 응답 샘플 사용

### 4. 회복탄력성 테스트 (Resilience Tests)
- **목적**: 서킷브레이커, 재시도 등 정책 검증
- **방법**: Mock 서버로 실패 시나리오 시뮬레이션
- **도구**: WireMock, TestContainers

## 🚨 주의사항

### 1. 보안
- **자격증명**: 절대 로그나 코드에 하드코딩 금지
- **마스킹**: 민감한 정보는 일관되게 마스킹
- **암호화**: 전송 중 데이터 암호화 필수

### 2. 성능
- **페이징**: 대량 조회 시 반드시 페이징 적용
- **캐싱**: 자주 조회되는 데이터는 캐싱 고려
- **비동기**: 장시간 소요 작업은 비동기 처리

### 3. 모니터링
- **메트릭**: 모든 CSP 연동에 대한 메트릭 수집
- **알림**: 실패율 임계치 초과 시 알림 설정
- **로그**: 구조화된 로그로 디버깅 용이성 확보

## 📖 추가 학습 자료

### 헥사고날 아키텍처
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Ports and Adapters Pattern](https://herbertograca.com/2017/09/14/ports-adapters-architecture/)

### Spring Boot + 헥사고날
- [Spring Boot Hexagonal Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Implementing Hexagonal Architecture with Spring Boot](https://reflectoring.io/spring-hexagonal/)

### 멀티 클라우드 패턴
- [Multi-Cloud Architecture Patterns](https://aws.amazon.com/architecture/well-architected/)
- [Cloud Provider Integration Best Practices](https://docs.microsoft.com/en-us/azure/architecture/guide/)

---

