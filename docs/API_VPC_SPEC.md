# VPC 관리 API 스펙

## 개요

VPC(가상 사설 클라우드) 생성/조회/수정/삭제를 위한 REST API입니다. 헥사고날 아키텍처 기반으로 유즈케이스 → 포트 라우터 → CSP별 어댑터(AWS: Ec2Client) 흐름으로 처리되며, 모든 호출은 감사를 위해 AOP 기반 Audit 로깅이 적용됩니다.

## 기본 정보

- **Base URL**: `/api/v1/vpcs`
- **인증**: Bearer Token (JWT)
- **테넌트 헤더(선택/정책에 따라 필수 가능)**: `X-Tenant-Key`
- **Content-Type**: `application/json`

## 응답 형식

- **성공**: 컨트롤러가 도메인 객체를 직접 반환합니다
  - 생성/단건/수정: `CloudResource`
  - 목록: `List<CloudResource>`
- **에러**: 전역 예외 처리기(GlobalExceptionHandler)가 표준 에러 바디를 반환합니다
  - 형식: `ApiResponse.error`
  - HTTP 상태/에러 코드는 예외/에러코드 매핑에 따름

## 엔드포인트

### 1) VPC 생성
- **URL**: `POST /api/v1/vpcs`
- **설명**: VPC를 생성합니다 (AWS의 경우 Ec2Client.createVpc 호출)
- **헤더**:
  - `Authorization: Bearer {token}`
  - `X-Tenant-Key: {tenantKey}` (optional)
- **요청 본문 (예: VpcCreateRequest)**
```json
{
  "providerType": "AWS",
  "accountScope": "123456789012",
  "region": "us-east-1",
  "vpcName": "test-vpc",
  "cidrBlock": "10.0.0.0/16",
  "description": "Test VPC",
  "tenantKey": "tenant-001"
}
```
- **응답 예시 (201 Created)**
```json
{
  "resourceId": "vpc-12345678",
  "resourceName": "test-vpc",
  "status": "ACTIVE",
  "provider": {
    "providerKey": "aws",
    "providerName": "Amazon Web Services",
    "providerType": "AWS"
  },
  "metadata": "..."
}
```
- **에러 응답**: 전역 예외 처리기에서 매핑 (예: 검증 400, 권한 401/403, 내부 500)
- **감사 로그(AOP)**: `action=CREATE_VPC`, `resourceType=CLOUD_PROVIDER`, `severity=MEDIUM`, 요청/응답 데이터 포함

---

### 2) VPC 단일 조회
- **URL**: `GET /api/v1/vpcs/{providerType}/{accountScope}/{region}/{resourceId}`
- **설명**: 특정 VPC를 조회합니다
- **경로 변수**:
  - `providerType` (예: `AWS`)
  - `accountScope` (예: `123456789012`)
  - `region` (예: `us-east-1`)
  - `resourceId` (예: `vpc-0abc1234def567890`)
- **응답**:
  - 200 OK (존재 시)
  - 404 Not Found (미존재 시)
- **응답 예시 (200 OK)**
```json
{
  "resourceId": "vpc-0abc1234def567890",
  "resourceName": "prod-vpc",
  "status": "ACTIVE",
  "provider": {
    "providerKey": "aws",
    "providerName": "Amazon Web Services",
    "providerType": "AWS"
  },
  "metadata": "..."
}
```
- **감사 로그(AOP)**: `action=GET_VPC`, `resourceType=CLOUD_PROVIDER`, `severity=LOW`, 응답 데이터 포함
- **에러 응답**:
  - 404 Not Found: 컨트롤러에서 empty일 때 반환
  - 그 외 예외는 전역 예외 처리기에서 매핑

---

### 3) VPC 목록 조회
- **URL**: `GET /api/v1/vpcs`
- **설명**: 조건에 맞는 VPC 목록을 조회합니다
- **쿼리 파라미터 (예: VpcQuery)**
  - `providerType` (예: `AWS`)
  - `vpcName` (태그 `Name` 기반 필터링)
  - `cidrBlock`
- **응답 예시 (200 OK)**
```json
[
  {
    "resourceId": "vpc-12345678",
    "resourceName": "test-vpc",
    "status": "ACTIVE"
  },
  {
    "resourceId": "vpc-87654321",
    "resourceName": "stage-vpc",
    "status": "ACTIVE"
  }
]
```
- **감사 로그(AOP)**: `action=LIST_VPCS`, `resourceType=CLOUD_PROVIDER`, `severity=LOW`
- **에러 응답**: 전역 예외 처리기에서 매핑

---

### 4) VPC 수정
- **URL**: `PUT /api/v1/vpcs/{providerType}/{accountScope}/{region}/{resourceId}`
- **설명**: VPC 속성을 수정합니다 (AWS는 주로 태그 업데이트로 처리)
- **경로 변수**: `providerType`, `accountScope`, `region`, `resourceId`
- **요청 본문 (예: VpcUpdateRequest)**
```json
{
  "tags": {
    "Name": "new-name",
    "Environment": "prod"
  }
}
```
- **응답 예시 (200 OK)**
```json
{
  "resourceId": "vpc-12345678",
  "resourceName": "new-name",
  "status": "ACTIVE",
  "metadata": "..."
}
```
- **에러 응답**: 전역 예외 처리기에서 매핑
- **감사 로그(AOP)**: `action=UPDATE_VPC`, `resourceType=CLOUD_PROVIDER`, `severity=MEDIUM`, 요청/응답 데이터 포함

---

### 5) VPC 삭제
- **URL**: `DELETE /api/v1/vpcs/{providerType}/{accountScope}/{region}/{resourceId}`
- **설명**: 특정 VPC를 삭제합니다
- **경로 변수**: `providerType`, `accountScope`, `region`, `resourceId`
- **응답**: `204 No Content`
- **에러 응답**: 전역 예외 처리기에서 매핑
- **감사 로그(AOP)**: `action=DELETE_VPC`, `resourceType=CLOUD_PROVIDER`, `severity=HIGH`, 요청 데이터 포함

## 보안 및 권한
- 모든 API는 JWT 인증을 전제로 합니다.
- 테넌트 격리가 활성화된 경우 `X-Tenant-Key` 헤더가 요구될 수 있습니다.
- 역할/권한 정책과 연계 시, 컨트롤러 진입 전 AOP 또는 Security Filter에서 접근 통제됩니다.

## 에러 처리

- 전역 예외 처리: `common/exception/GlobalExceptionHandler`
- 매핑 원칙(요약):
  - 인증/인가: 401/403
  - 유효성 검증: 400 (필드 에러 포함 가능)
  - 데이터 접근/내부 오류: 500
  - Not Found는 컨트롤러에서 직접 `404` 반환하는 케이스가 존재(getVpc)

## 구현 참고

### 아키텍처
- **컨트롤러**: `src/main/java/com/agenticcp/core/domain/cloud/controller/VpcController.java`
- **유즈케이스 서비스**: `src/main/java/com/agenticcp/core/domain/cloud/port/outbound/vpc/VpcUseCaseService.java`
- **포트 라우터**: `src/main/java/com/agenticcp/core/domain/cloud/port/outbound/vpc/VpcPortRouter.java`
- **포트 인터페이스**: `src/main/java/com/agenticcp/core/domain/cloud/port/outbound/vpc/VpcManagementPort.java`
- **AWS 어댑터**: `src/main/java/com/agenticcp/core/domain/cloud/adapter/outbound/aws/vpc/AwsVpcManagementAdapter.java`
- **매퍼**: `src/main/java/com/agenticcp/core/domain/cloud/adapter/outbound/aws/vpc/AwsVpcMapper.java`

### 주요 설계 패턴

#### Command 패턴
- DTO와 포트 모델 간 의존성을 분리하기 위해 Command 객체 사용
- Command 클래스:
  - `CreateVpcCommand`: VPC 생성 명령
  - `GetVpcCommand`: VPC 단건 조회 명령
  - `ListVpcsQuery`: VPC 목록 조회 쿼리
  - `UpdateVpcCommand`: VPC 수정 명령
  - `DeleteVpcCommand`: VPC 삭제 명령
- 위치: `src/main/java/com/agenticcp/core/domain/cloud/port/command/vpc/`

#### 상수 관리
- `VpcConstants`: VPC 관련 공통 상수 정의
  - `SERVICE_KEY = "EC2"`: AWS EC2 서비스 키
  - `RESOURCE_TYPE = "VPC"`: VPC 리소스 타입
- 위치: `src/main/java/com/agenticcp/core/domain/cloud/port/outbound/vpc/VpcConstants.java`

#### CloudResource 매핑 개선
- `AwsVpcMapper`에서 `CloudResource`의 `provider`, `service`, `region` 필드를 JPA 엔티티로 조회
- Repository 의존성:
  - `CloudProviderRepository`: CloudProvider 엔티티 조회
  - `CloudServiceRepository`: CloudService 엔티티 조회 (신규 생성)
  - `CloudRegionRepository`: CloudRegion 엔티티 조회 (findByProviderTypeAndRegionKey 메서드 추가)
- 매핑 로직:
  - AWS VPC 응답을 `CloudResource`로 변환 시 관련 엔티티를 조회하여 설정
  - 태그에서 리소스 이름 추출 (Name 태그 우선)
  - 태그 및 메타데이터를 JSON 문자열로 변환
  - 생명주기 상태 매핑

### 데이터 흐름
1. **Controller** → DTO 검증 및 `ResourceIdentity` 생성
2. **VpcUseCaseService** → DTO를 Command로 변환, `CapabilityGuard` 검증
3. **VpcPortRouter** → ProviderType에 따라 적절한 어댑터 선택
4. **AwsVpcManagementAdapter** → AWS SDK 호출 (Ec2Client)
5. **AwsVpcMapper** → AWS 응답을 `CloudResource`로 변환 (엔티티 조회 포함)
6. **Controller** → `CloudResource` 반환


