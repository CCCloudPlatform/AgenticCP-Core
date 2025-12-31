# AWS 계정 연결 및 리소스 생성 가이드

이 문서는 AWS 계정 연결부터 리소스 생성까지의 전체 플로우를 설명합니다.
조직과 사용자 간의 가이드는 기능 통합 후 추가 예정


## 목차

1. [인증 및 기본 설정](#1-인증-및-기본-설정)
2. [AWS 계정 사전 설정](#2-aws-계정-사전-설정)
3. [프로바이더 생성](#3-프로바이더-생성)
4. [AWS 계정 등록](#4-aws-계정-등록)
5. [계정 관리](#5-계정-관리)
6. [사용자 역할 관리](#6-사용자-역할-관리)
7. [리소스 생성](#7-리소스-생성)
8. [에러 처리](#8-에러-처리)

---

## 1. 인증 및 기본 설정

### 1.1 로그인

**엔드포인트**: `POST /api/v1/auth/login`

**요청 예시**:
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**응답 예시**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "ACCESS",
  "expiresIn": 3600,
  "refreshExpiresIn": 604800
}
```

### 1.2 요청 헤더 설정

모든 API 요청에 다음 헤더를 포함해야 합니다:

```javascript
{
  "Authorization": "Bearer {accessToken}",
  "X-Tenant-Key": "{tenantKey}",  // JWT 토큰의 tenantKey와 일치해야 함
  "Content-Type": "application/json"
}
```

**주의사항**:
- `X-Tenant-Key` 헤더는 JWT 토큰에 포함된 `tenantKey`와 일치해야 합니다.
- 헤더가 없거나 불일치하면 `테넌트 컨텍스트가 설정되지 않았습니다.` 오류가 발생합니다.

---

## 2. AWS 계정 사전 설정

AgenticCP에 AWS 계정을 등록하기 전에 AWS 콘솔에서 IAM 사용자를 생성하고 Access Key를 발급받아야 합니다.

### 2.1 AWS 계정 준비

1. **AWS 계정 확인**
   - AWS 계정이 있어야 합니다. (없는 경우 [AWS 가입](https://aws.amazon.com/ko/))
   - AWS Account ID를 확인합니다. (AWS 콘솔 우측 상단에서 확인 가능)

2. **IAM 서비스 접근**
   - AWS 콘솔에서 IAM 서비스로 이동합니다.

### 2.2 IAM 사용자 생성

1. **IAM 사용자 생성**
   - IAM 콘솔 → 사용자 → 사용자 추가
   - 사용자 이름 입력 (예: `agenticcp-service-user`)
   - "AWS 자격 증명 유형" 선택: **액세스 키 - 프로그래밍 방식 액세스**

2. **권한 정책 할당**

   **최소 권한 정책** (S3 Bucket 생성/관리용):
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:CreateBucket",
           "s3:DeleteBucket",
           "s3:ListBucket",
           "s3:GetBucketLocation",
           "s3:GetBucketAcl",
           "s3:PutBucketAcl",
           "s3:PutBucketOwnershipControls",
           "s3:PutBucketVersioning",
           "s3:PutBucketTagging",
           "s3:GetBucketTagging",
           "s3:PutBucketPolicy",
           "s3:GetBucketPolicy",
           "sts:GetCallerIdentity"
         ],
         "Resource": "*"
       }
     ]
   }
   ```

   **권장 정책** (모든 클라우드 리소스 관리용):
   - `AmazonS3FullAccess`: S3 전체 권한
   - `AmazonEC2FullAccess`: EC2 전체 권한 (VM 생성 시 필요)
   - `AmazonVPCFullAccess`: VPC 전체 권한 (VPC 생성 시 필요)
   - 또는 사용자 정의 정책으로 필요한 권한만 선택

3. **태그 추가** (선택 사항)
   - 사용자에 태그를 추가하여 관리할 수 있습니다.

4. **사용자 생성 완료**

### 2.3 Access Key 발급

1. **Access Key 다운로드**
   - 사용자 생성 완료 후 **Access Key ID**와 **Secret Access Key**가 표시됩니다.
   - **중요**: Secret Access Key는 이 시점에만 표시되므로 반드시 저장하세요.

2. **Access Key 저장**
   - Access Key ID: `AKIAIOSFODNN7EXAMPLE` (예시)
   - Secret Access Key: `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` (예시)
   - 안전한 곳에 보관하세요. (비밀번호 관리자 등)

3. **기존 사용자의 Access Key 발급** (이미 사용자가 있는 경우)
   - IAM 콘솔 → 사용자 → 해당 사용자 선택
   - 보안 자격 증명 탭 → 액세스 키 만들기
   - 프로그래밍 방식 액세스 선택

### 2.4 보안 모범 사례

1. **최소 권한 원칙**
   - 필요한 최소한의 권한만 부여하세요.
   - 모든 리소스(`*`)에 대한 권한은 피하세요.

2. **정기적인 키 로테이션**
   - Access Key는 정기적으로 교체하세요. (권장: 90일마다)
   - 오래된 키는 비활성화하거나 삭제하세요.

3. **MFA 활성화** (선택 사항)
   - IAM 사용자에 MFA를 활성화하여 보안을 강화할 수 있습니다.

4. **키 보안 관리**
   - Secret Access Key는 절대 코드에 하드코딩하지 마세요.
   - 환경 변수나 비밀 관리 서비스를 사용하세요.

### 2.5 필요한 정보 확인

계정 등록 전에 다음 정보를 준비하세요:

- ✅ **Access Key ID**: IAM 사용자의 Access Key ID
- ✅ **Secret Access Key**: IAM 사용자의 Secret Access Key
- ✅ **AWS Account ID**: 12자리 숫자 (예: `123456789012`)
- ✅ **기본 리전**: 사용할 AWS 리전 (예: `ap-northeast-2`)

**AWS Account ID 확인 방법**:
- AWS 콘솔 우측 상단에서 계정 이름 클릭 → 계정 ID 표시
- 또는 다음 명령어로 확인:
  ```bash
  aws sts get-caller-identity --query Account --output text
  ```

---

## 3. 프로바이더 생성

### 3.1 프로바이더 조회

계정 등록 전에 프로바이더가 이미 존재하는지 확인할 수 있습니다.

**엔드포인트**: `GET /api/cloud/providers`

**응답 예시**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "providerKey": "aws",
      "providerName": "Amazon Web Services",
      "providerType": "AWS",
      "status": "ACTIVE",
      "description": "Amazon Web Services 클라우드 플랫폼",
      "apiEndpoint": "https://aws.amazon.com",
      "supportedRegions": ["us-east-1", "ap-northeast-2", ...],
      "createdAt": "2025-12-19T10:00:00.000000"
    }
  ],
  "timestamp": "2025-12-19T10:00:00.000000"
}
```

**활성 프로바이더만 조회**:
- `GET /api/cloud/providers/active`

**프로바이더 타입별 조회**:
- `GET /api/cloud/providers/type/AWS`

### 3.2 프로바이더 생성

프로바이더가 존재하지 않는 경우 생성해야 합니다.

**엔드포인트**: `POST /api/cloud/providers`

**요청 예시**:
```json
{
  "providerKey": "aws",
  "providerName": "Amazon Web Services",
  "description": "Amazon Web Services 클라우드 플랫폼",
  "providerType": "AWS",
  "status": "ACTIVE",
  "apiEndpoint": "https://aws.amazon.com",
  "apiVersion": "latest",
  "authenticationType": "ACCESS_KEY",
  "supportedRegions": [
    "us-east-1",
    "us-west-2",
    "ap-northeast-2",
    "ap-southeast-1"
  ],
  "supportedServices": [
    "S3",
    "EC2",
    "VPC",
    "RDS"
  ],
  "pricingModel": "PAY_AS_YOU_GO",
  "isGlobal": true,
  "isGovernment": false
}
```

**필수 필드**:
- `providerKey`: 프로바이더 키 (고유 식별자, 예: `"aws"`)
- `providerName`: 프로바이더 이름 (예: `"Amazon Web Services"`)
- `providerType`: 프로바이더 타입 (`"AWS"`, `"AZURE"`, `"GCP"`)

**선택 필드**:
- `description`: 프로바이더 설명
- `status`: 상태 (`"ACTIVE"`, `"INACTIVE"`, 기본값: `"ACTIVE"`)
- `apiEndpoint`: API 엔드포인트 URL
- `apiVersion`: API 버전
- `authenticationType`: 인증 타입 (`"ACCESS_KEY"`, `"OAUTH"`, `"SERVICE_ACCOUNT"`)
- `supportedRegions`: 지원 리전 목록
- `supportedServices`: 지원 서비스 목록
- `pricingModel`: 가격 모델 (`"PAY_AS_YOU_GO"`, `"RESERVED"`, `"SPOT"`)
- `isGlobal`: 글로벌 프로바이더 여부
- `isGovernment`: 정부용 프로바이더 여부

**응답 예시** (201 Created):
```json
{
  "success": true,
  "message": "클라우드 프로바이더가 생성되었습니다.",
  "data": {
    "id": 1,
    "providerKey": "aws",
    "providerName": "Amazon Web Services",
    "providerType": "AWS",
    "status": "ACTIVE",
    "createdAt": "2025-12-19T10:00:00.000000"
  },
  "timestamp": "2025-12-19T10:00:00.000000"
}
```

**주의사항**:
- 프로바이더는 일반적으로 시스템 관리자가 초기 설정 시 생성합니다.
- 프로바이더가 `ACTIVE` 상태여야 계정 등록이 가능합니다.
- `providerKey`는 고유해야 하며, 이미 존재하는 경우 중복 오류가 발생합니다.

### 3.3 프로바이더 활성화

프로바이더가 `INACTIVE` 상태인 경우 활성화해야 합니다.

**엔드포인트**: `PATCH /api/cloud/providers/{providerKey}/activate`

**예시**: `PATCH /api/cloud/providers/aws/activate`

---

## 4. AWS 계정 등록

### 4.1 사전 검증 (선택 사항)

계정 등록 전에 자격증명을 먼저 검증할 수 있습니다.

**엔드포인트**: `POST /api/v1/cloud/accounts/validate`

**요청 예시**:
```json
{
  "providerType": "AWS",
  "accessKeyId": "{accessKey}",
  "secretAccessKey": "{secretAccessKey}",
  "region": "ap-northeast-2"
}
```

**응답 예시** (성공):
```json
{
  "valid": true,
  "message": "계정 검증에 성공했습니다.",
  "accountScope": "123456789012",
  "region": "ap-northeast-2"
}
```

**응답 예시** (실패):
```json
{
  "valid": false,
  "message": "AWS 계정 검증 실패: Invalid credentials",
  "accountScope": null,
  "region": null
}
```

### 4.2 계정 등록

**엔드포인트**: `POST /api/v1/cloud/accounts`

**요청 예시**:
```json
{
  "providerType": "AWS",
  "accountName": "My AWS Account",
  "accountScope": "123456789012",
  "accessKey": "{accessKey}",
  "secretKey": "{secretAccessKey}",
  "region": "ap-northeast-2",
  "isDefault": true
}
```

**필드 설명**:
- `providerType`: `"AWS"` (필수)
- `accountName`: 사용자가 지정하는 계정 이름 (2-100자, 필수)
- `accountScope`: AWS Account ID (12자리 숫자, 선택 사항 - 검증 시 자동 추출됨)
- `accessKey`: AWS Access Key ID (필수)
- `secretKey`: AWS Secret Access Key (필수)
- `region`: 기본 리전 (예: `"ap-northeast-2"`, 선택 사항)
- `isDefault`: 기본 계정으로 설정 여부 (선택 사항)

**응답 예시** (201 Created):
```json
{
  "id": 1,
  "tenantId": 3,
  "tenantKey": "test-project-tenant-key",
  "providerId": 4,
  "providerType": "AWS",
  "providerName": "AWS",
  "accountName": "My AWS Account",
  "accountScope": "123456789012",
  "accountStatus": "VERIFIED",
  "isDefault": true,
  "region": "ap-northeast-2",
  "verifiedAt": "2025-12-19T10:23:29.652362",
  "createdAt": "2025-12-19T10:23:29.660865",
  "updatedAt": "2025-12-19T10:23:29.660865"
}
```

**등록 프로세스**:
1. 프로바이더 조회 및 검증 (프로바이더가 `ACTIVE` 상태여야 함)
2. 자격증명 검증 (AWS STS `GetCallerIdentity` API 호출)
3. Account ID 자동 추출 (accountScope가 제공되지 않은 경우)
4. 자격증명 암호화 저장
5. CloudAccount 엔티티 생성

**주의사항**:
- 계정 등록 전에 프로바이더가 존재하고 `ACTIVE` 상태여야 합니다.
- `region` 필드에 `=` 기호가 포함되면 안 됩니다. (예: `"ap-northeast=2"` ❌ → `"ap-northeast-2"` ✅)
- 동일한 테넌트, 프로바이더, 계정 스코프 조합은 중복 등록할 수 없습니다.

---

## 5. 계정 관리

### 5.1 계정 목록 조회

**엔드포인트**: `GET /api/v1/cloud/accounts`

**응답 예시**:
```json
[
  {
    "id": 1,
    "accountName": "My AWS Account",
    "accountScope": "123456789012",
    "providerType": "AWS",
    "accountStatus": "VERIFIED",
    "isDefault": true,
    "region": "ap-northeast-2"
  }
]
```

### 5.2 특정 계정 조회

**엔드포인트**: `GET /api/v1/cloud/accounts/{accountId}`

### 5.3 계정 연결 테스트

**엔드포인트**: `POST /api/v1/cloud/accounts/{accountId}/test-connection`

**응답 예시**:
```json
{
  "success": true,
  "message": "연결 테스트에 성공했습니다.",
  "testedAt": "2025-12-19T10:30:00.000000"
}
```

### 5.4 프로바이더별 계정 조회

**엔드포인트**: `GET /api/v1/cloud/accounts/provider/{providerType}`

**예시**: `GET /api/v1/cloud/accounts/provider/AWS`

---

## 6. 사용자 역할 관리

리소스 생성을 위해서는 사용자에게 적절한 권한이 필요합니다. 권한은 역할(Role)을 통해 부여됩니다.

### 6.1 역할 조회

먼저 사용 가능한 역할을 확인합니다.

**엔드포인트**: `GET /api/v1/roles`

**응답 예시**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "roleKey": "OBJECT_STORAGE_ADMIN",
      "roleName": "오브젝트 스토리지 관리자",
      "description": "오브젝트 스토리지 컨테이너의 생성, 조회, 수정, 삭제 권한을 가진 역할",
      "priority": 85,
      "permissions": [
        {
          "permissionKey": "OBJECT_STORAGE_CONTAINER_CREATE",
          "permissionName": "Object Storage Container 생성"
        },
        {
          "permissionKey": "OBJECT_STORAGE_CONTAINER_READ",
          "permissionName": "Object Storage Container 조회"
        },
        {
          "permissionKey": "OBJECT_STORAGE_CONTAINER_UPDATE",
          "permissionName": "Object Storage Container 수정"
        },
        {
          "permissionKey": "OBJECT_STORAGE_CONTAINER_DELETE",
          "permissionName": "Object Storage Container 삭제"
        }
      ]
    }
  ]
}
```

### 6.2 사용자에게 역할 할당

S3 Bucket을 생성하려면 사용자에게 `OBJECT_STORAGE_ADMIN` 역할을 할당해야 합니다.

**엔드포인트**: `POST /api/v1/roles/users/{username}`

**URL 파라미터**:
- `username`: 역할을 할당할 사용자명

**요청 예시**:
```json
["OBJECT_STORAGE_ADMIN"]
```

**여러 역할 동시 할당**:
```json
["OBJECT_STORAGE_ADMIN", "CLOUD_ADMIN"]
```

**응답 예시** (200 OK):
```json
{
  "success": true,
  "message": "역할이 할당되었습니다.",
  "data": null,
  "timestamp": "2025-12-19T11:00:00.000000"
}
```

**권한 요구사항**:
- `USER_UPDATE` 권한 또는 `SUPER_ADMIN` 역할 필요

**주의사항**:
- 역할 할당은 기존 역할을 모두 교체합니다. (추가가 아님)
- 여러 역할을 할당하려면 모든 역할 키를 배열에 포함해야 합니다.

### 6.3 사용자에서 역할 제거

**엔드포인트**: `DELETE /api/v1/roles/users/{username}/{roleKey}`

**URL 파라미터**:
- `username`: 역할을 제거할 사용자명
- `roleKey`: 제거할 역할 키 (예: `OBJECT_STORAGE_ADMIN`)

**예시**: `DELETE /api/v1/roles/users/testuser/OBJECT_STORAGE_ADMIN`

**응답 예시** (200 OK):
```json
{
  "success": true,
  "message": "역할이 제거되었습니다.",
  "data": null,
  "timestamp": "2025-12-19T11:05:00.000000"
}
```

### 6.4 역할 생성 (선택 사항)

필요한 권한을 가진 역할이 없는 경우 새로 생성할 수 있습니다.

**엔드포인트**: `POST /api/v1/roles`

**요청 예시**:
```json
{
  "roleKey": "OBJECT_STORAGE_ADMIN",
  "roleName": "오브젝트 스토리지 관리자",
  "description": "오브젝트 스토리지 컨테이너의 생성, 조회, 수정, 삭제 권한을 가진 역할",
  "priority": 85,
  "permissionKeys": [
    "OBJECT_STORAGE_CONTAINER_CREATE",
    "OBJECT_STORAGE_CONTAINER_READ",
    "OBJECT_STORAGE_CONTAINER_UPDATE",
    "OBJECT_STORAGE_CONTAINER_DELETE"
  ],
  "isSystem": false
}
```

**필드 설명**:
- `roleKey`: 역할 키 (고유 식별자, 필수)
- `roleName`: 역할 이름 (필수)
- `description`: 역할 설명 (선택 사항)
- `priority`: 우선순위 (숫자가 클수록 높음, 선택 사항)
- `permissionKeys`: 권한 키 목록 (필수)
- `isSystem`: 시스템 역할 여부 (기본값: `false`)

**주의사항**:
- 역할 생성 전에 필요한 권한이 먼저 생성되어 있어야 합니다.
- 권한이 없는 경우 `PERMISSION_NOT_FOUND` 오류가 발생합니다.

---

## 7. 리소스 생성

### 7.1 S3 Bucket (Object Storage Container) 생성

**엔드포인트**: `POST /api/v1/cloud/providers/{provider}/accounts/{accountScope}/storage/containers`

**URL 파라미터**:
- `provider`: `"AWS"` (필수)
- `accountScope`: AWS Account ID (12자리 숫자, 필수)

**요청 예시**:
```json
{
  "containerName": "my-s3-bucket-123",
  "region": "ap-northeast-2",
  "objectOwnership": "BucketOwnerEnforced",
  "objectLockEnabled": false,
  "tags": {
    "Environment": "production",
    "Project": "agenticcp",
    "Owner": "dev-team"
  }
}
```

**필드 설명**:
- `containerName`: S3 Bucket 이름 (3-63자, 소문자/숫자/하이픈만 허용, 필수)
- `region`: AWS 리전 (예: `"ap-northeast-2"`, 필수)
- `objectOwnership`: 객체 소유권 설정 (선택 사항)
  - `"BucketOwnerEnforced"`: ACL 비활성화, Bucket 소유자가 모든 객체 소유 (권장)
  - `"ObjectWriter"`: 업로더가 객체 소유
- `objectLockEnabled`: 객체 잠금 활성화 여부 (기본값: `false`, 선택 사항)
  - `true`로 설정 시 WORM(Write Once Read Many) 활성화 (생성 후 변경 불가)
- `tags`: 태그 (선택 사항)

**응답 예시** (201 Created):
```json
{
  "success": true,
  "message": "Object Storage Container 생성에 성공했습니다.",
  "data": {
    "id": 1,
    "resourceId": "my-s3-bucket-123",
    "resourceType": "OBJECT_STORAGE_CONTAINER",
    "resourceName": "my-s3-bucket-123",
    "providerType": "AWS",
    "accountScope": "123456789012",
    "region": "ap-northeast-2",
    "status": "ACTIVE",
    "createdAt": "2025-12-19T10:35:00.000000"
  },
  "timestamp": "2025-12-19T10:35:00.000000"
}
```

**주의사항**:
- S3 Bucket 이름은 전 세계적으로 고유해야 합니다.
- Bucket 이름 규칙:
  - 3-63자 사이
  - 소문자, 숫자, 하이픈(`-`)만 허용
  - 시작과 끝은 문자나 숫자여야 함
  - IP 주소 형식 불가
  - `xn--` 또는 `sthree-`로 시작 불가

### 7.2 Container 목록 조회

**엔드포인트**: `GET /api/v1/cloud/providers/{provider}/accounts/{accountScope}/storage/containers`

**쿼리 파라미터** (선택 사항):
- `region`: 리전 필터
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)

**예시**: `GET /api/v1/cloud/providers/AWS/accounts/123456789012/storage/containers?region=ap-northeast-2&page=0&size=20`

### 7.3 Container 조회

**엔드포인트**: `GET /api/v1/cloud/providers/{provider}/accounts/{accountScope}/storage/containers/{containerName}`

### 7.4 Container 업데이트

**엔드포인트**: `PUT /api/v1/cloud/providers/{provider}/accounts/{accountScope}/storage/containers/{containerName}`

**요청 예시**:
```json
{
  "objectOwnership": "BucketOwnerEnforced",
  "tags": {
    "Environment": "staging",
    "Updated": "2025-12-19"
  }
}
```

### 7.5 Container 삭제

**엔드포인트**: `DELETE /api/v1/cloud/providers/{provider}/accounts/{accountScope}/storage/containers/{containerName}`

**주의사항**: 
- S3 Bucket이 비어있지 않으면 삭제할 수 없습니다.
- 먼저 모든 객체를 삭제한 후 Bucket을 삭제해야 합니다.

---

## 8. 에러 처리

### 8.1 공통 에러 응답 형식

모든 에러는 다음 형식으로 반환됩니다:

```json
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE",
  "timestamp": "2025-12-19T10:40:00.000000"
}
```

### 8.2 주요 에러 코드

#### 인증/권한 관련

- `COMMON_4`: 테넌트 컨텍스트가 설정되지 않았습니다.
  - **원인**: `X-Tenant-Key` 헤더가 없거나 JWT 토큰의 `tenantKey`와 불일치
  - **해결**: 올바른 `X-Tenant-Key` 헤더를 포함하세요.

- `403 Forbidden`: 권한 없음
  - **원인**: 필요한 권한이 없음
  - **해결**: 사용자에게 적절한 역할/권한을 할당하세요.

#### 프로바이더 관련

- `CLOUD_PROVIDER_NOT_FOUND`: 프로바이더를 찾을 수 없습니다.
  - **원인**: 계정 등록 시 지정한 `providerType`에 해당하는 프로바이더가 없음
  - **해결**: 먼저 프로바이더를 생성하거나 활성화하세요.

#### 계정 등록 관련

- `CLOUD_4013`: AWS 계정 검증 실패
  - **원인**: 잘못된 자격증명 또는 잘못된 리전 형식
  - **해결**: 
    - Access Key/Secret Key 확인
    - 리전 형식 확인 (예: `"ap-northeast-2"` ✅, `"ap-northeast=2"` ❌)

- `CLOUD_4001`: 계정 중복
  - **원인**: 동일한 테넌트, 프로바이더, 계정 스코프 조합이 이미 존재
  - **해결**: 다른 계정 스코프를 사용하거나 기존 계정을 수정/삭제하세요.

#### 리소스 생성 관련

- `CLOUD_4040`: 리소스 생성 후 DB 저장 실패
  - **원인**: CloudService 엔티티가 없거나 제약 조건 위반
  - **해결**: 시스템 관리자에게 문의하세요.

- `409 Conflict`: Container 이름 중복
  - **원인**: S3 Bucket 이름이 이미 존재함 (전 세계적으로 고유해야 함)
  - **해결**: 다른 Bucket 이름을 사용하세요.

### 8.3 HTTP 상태 코드

- `200 OK`: 성공
- `201 Created`: 리소스 생성 성공
- `400 Bad Request`: 잘못된 요청 데이터
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스를 찾을 수 없음
- `409 Conflict`: 리소스 충돌 (중복 등)
- `500 Internal Server Error`: 서버 내부 오류

---

## 9. 체크리스트

프론트엔드 개발 시 확인사항:

**사전 준비**:
- [ ] AWS 계정 보유 및 Account ID 확인
- [ ] IAM 사용자 생성 및 Access Key 발급 완료
- [ ] 필요한 권한 정책이 IAM 사용자에 할당됨

**API 요청**:
- [ ] 모든 요청에 `Authorization` 헤더 포함
- [ ] 모든 요청에 `X-Tenant-Key` 헤더 포함 (JWT의 `tenantKey`와 일치)
- [ ] 계정 등록 전 프로바이더 존재 여부 확인
- [ ] 프로바이더가 `ACTIVE` 상태인지 확인
- [ ] 리소스 생성 전 사용자에게 필요한 역할 할당 확인
- [ ] AWS 리전 형식 확인 (`ap-northeast-2` ✅, `ap-northeast=2` ❌)
- [ ] S3 Bucket 이름 규칙 준수 (3-63자, 소문자/숫자/하이픈만)
- [ ] 에러 응답 처리 구현
- [ ] 로딩 상태 표시
- [ ] 사용자 권한 확인 (필요한 권한이 있는지)

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2025-12-19

