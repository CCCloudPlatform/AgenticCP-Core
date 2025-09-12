# AgenticCP-Core

CloudPlatform 2.0 Core Application - Spring Boot 기반의 멀티 클라우드 플랫폼 통합 관리 시스템

## 🚀 기술 스택

- **Backend**: Spring Boot 3.3.5, Java 17
- **Database**: MySQL 8.0.33
- **Container**: Docker, Docker Compose
- **Build Tool**: Maven
- **ORM**: Spring Data JPA, Hibernate
- **Security**: Spring Security
- **Documentation**: Springdoc OpenAPI (Swagger UI)
- **Caching**: Spring Cache, Redis (예정)

## 📋 주요 기능

### 🏢 멀티 테넌트 아키텍처
- 테넌트별 격리된 리소스 관리
- 조직 및 사용자 계층 구조
- 역할 기반 접근 제어 (RBAC)

### ☁️ 멀티 클라우드 지원
- AWS, Azure, GCP 등 주요 클라우드 프로바이더 통합
- 클라우드 리소스 통합 관리
- 크로스 클라우드 모니터링

### 🔒 보안 및 컴플라이언스
- 포괄적인 보안 정책 관리
- 위협 탐지 및 대응
- 감사 로그 및 컴플라이언스

### ⚙️ 플랫폼 관리
- 기능 플래그 관리
- 플랫폼 설정 중앙화
- 비용 최적화 및 모니터링

## 🛠️ 개발 환경 설정

### 사전 요구사항

- **Java**: 17 이상
- **Maven**: 3.6 이상
- **Docker**: 20.10 이상
- **Docker Compose**: 2.0 이상
- **Git**: 2.0 이상

### 🚀 빠른 시작

#### 방법 1: 하이브리드 모드 (권장) - 개발자 친화적
```bash
# 1. 저장소 클론
git clone <repository-url>
cd AgenticCP-Core

# 2. 자동 스크립트로 시작 (가장 간단)
# macOS/Linux
./start-dev.sh

# Windows
start-dev.bat
```

#### 방법 2: 전체 Docker 모드
```bash
# 1. 저장소 클론
git clone <repository-url>
cd AgenticCP-Core

# 2. Docker Compose로 전체 스택 실행
docker-compose up -d

# 3. 빌드와 함께 시작 (소스코드 변경 후)
docker-compose up --build -d

# 4. 로그 확인
docker-compose logs -f app
```

#### 방법 3: 수동 하이브리드 모드
```bash
# 1. MySQL과 phpMyAdmin만 Docker로 실행
docker-compose -f docker-compose.dev.yml up -d

# 2. 로컬에서 Spring Boot 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 🔧 개발 모드

#### 옵션 1: 하이브리드 모드 (권장) - MySQL + phpMyAdmin만 Docker
```bash
# 방법 1: 자동 스크립트 사용 (권장)
# macOS/Linux
./start-dev.sh

# Windows
start-dev.bat

# 방법 2: 수동 실행
# 1. MySQL과 phpMyAdmin만 Docker로 실행
docker-compose -f docker-compose.dev.yml up -d

# 2. 로컬에서 Spring Boot 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### 옵션 2: 전체 Docker 모드
```bash
# 소스코드 변경 후 재빌드
docker-compose up --build -d

# 특정 서비스만 재빌드
docker-compose up --build -d app
```

#### 옵션 3: 로컬 MySQL + 로컬 Spring Boot
```bash
# 로컬 MySQL 서버 실행 후
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 🎯 개발 모드 선택 가이드

| 모드 | 장점 | 단점 | 추천 상황 |
|------|------|------|-----------|
| **하이브리드 모드** | • 실시간 코드 반영<br>• 빠른 디버깅<br>• IDE 통합 개발 | • 로컬 Java 환경 필요 | **개발자 개인 개발** |
| **전체 Docker 모드** | • 환경 일관성<br>• 배포 환경과 동일<br>• 의존성 관리 간편 | • 코드 변경 시 재빌드 필요<br>• 디버깅 복잡 | **팀 공유 환경** |
| **로컬 모드** | • 최고 성능<br>• 완전한 제어 | • 환경 설정 복잡<br>• 의존성 충돌 가능 | **고급 개발자** |

### 🌐 접속 정보

| 서비스 | URL | 설명 |
|--------|-----|------|
| **애플리케이션** | http://localhost:8080/api | 메인 애플리케이션 |
| **Swagger UI** | http://localhost:8080/api/swagger-ui/index.html | API 문서 및 테스트 |
| **OpenAPI JSON** | http://localhost:8080/api/v3/api-docs | API 스펙 JSON |
| **phpMyAdmin** | http://localhost:8081 | 데이터베이스 관리 |
| **MySQL** | localhost:3306 | 데이터베이스 서버 |

### 🔐 Swagger UI 인증 테스트

Swagger UI에서 API 인증을 테스트할 수 있습니다:

1. **Swagger UI 접속**: http://localhost:8080/api/swagger-ui/index.html
2. **Authorize 버튼 클릭** (우상단)
3. **인증 방식 선택**:
   - **Bearer Token**: JWT 토큰 사용
   - **Basic Auth**: 사용자명/비밀번호
   - **API Key**: X-API-Key 헤더

#### JWT 토큰 테스트 방법
```bash
# 1. 로그인하여 토큰 발급
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 2. 응답에서 accessToken 복사
# 3. Swagger UI에서 Authorize → Bearer Token에 토큰 입력
```

### 🗄️ 데이터베이스 정보

| 항목 | 값 |
|------|-----|
| **데이터베이스명** | agenticcp |
| **사용자명** | agenticcp |
| **비밀번호** | agenticcppassword |
| **Root 비밀번호** | rootpassword |
| **포트** | 3306 |

## 📚 API 엔드포인트

### 🏥 헬스 체크
- `GET /api/health` - 애플리케이션 상태 확인
- `GET /api/health/ready` - 준비 상태 확인

### 🔐 인증 및 인가
- `POST /api/auth/login` - 사용자 로그인 (JWT 토큰 발급)
- `POST /api/auth/refresh` - 토큰 갱신
- `POST /api/auth/logout` - 로그아웃
- `GET /api/auth/me` - 현재 사용자 정보 조회

### 👥 사용자 관리
- `GET /api/users` - 모든 사용자 조회
- `POST /api/users` - 사용자 생성
- `GET /api/users/{username}` - 특정 사용자 조회
- `PUT /api/users/{username}` - 사용자 수정
- `DELETE /api/users/{username}` - 사용자 삭제
- `GET /api/users/active` - 활성 사용자 조회
- `GET /api/users/locked` - 잠긴 사용자 조회
- `GET /api/users/inactive` - 비활성 사용자 조회
- `GET /api/users/role/{role}` - 역할별 사용자 조회
- `GET /api/users/search` - 사용자 검색
- `PATCH /api/users/{username}/activate` - 사용자 활성화
- `PATCH /api/users/{username}/suspend` - 사용자 일시정지
- `PATCH /api/users/{username}/unlock` - 사용자 잠금 해제
- `PATCH /api/users/{username}/password` - 비밀번호 변경

### 🏢 테넌트 관리
- `GET /api/tenants` - 모든 테넌트 조회
- `POST /api/tenants` - 테넌트 생성
- `GET /api/tenants/{tenantKey}` - 특정 테넌트 조회
- `PUT /api/tenants/{tenantKey}` - 테넌트 수정
- `DELETE /api/tenants/{tenantKey}` - 테넌트 삭제
- `GET /api/tenants/active` - 활성 테넌트 조회
- `GET /api/tenants/type/{tenantType}` - 테넌트 타입별 조회
- `GET /api/tenants/trial/active` - 활성 트라이얼 테넌트 조회
- `GET /api/tenants/expired` - 만료된 테넌트 조회
- `GET /api/tenants/count/active` - 활성 테넌트 수 조회
- `PATCH /api/tenants/{tenantKey}/activate` - 테넌트 활성화
- `PATCH /api/tenants/{tenantKey}/suspend` - 테넌트 일시정지

### 🔒 보안 정책 관리
- `GET /api/security/policies` - 모든 보안 정책 조회
- `POST /api/security/policies` - 보안 정책 생성
- `GET /api/security/policies/{policyKey}` - 특정 보안 정책 조회
- `PUT /api/security/policies/{policyKey}` - 보안 정책 수정
- `DELETE /api/security/policies/{policyKey}` - 보안 정책 삭제
- `GET /api/security/policies/active` - 활성 보안 정책 조회
- `GET /api/security/policies/global` - 글로벌 보안 정책 조회
- `GET /api/security/policies/system` - 시스템 보안 정책 조회
- `GET /api/security/policies/effective` - 유효한 보안 정책 조회
- `GET /api/security/policies/type/{policyType}` - 정책 타입별 조회
- `PATCH /api/security/policies/{policyKey}/activate` - 보안 정책 활성화
- `PATCH /api/security/policies/{policyKey}/deactivate` - 보안 정책 비활성화
- `PATCH /api/security/policies/{policyKey}/toggle` - 보안 정책 토글

### ☁️ 클라우드 프로바이더 관리
- `GET /api/cloud/providers` - 모든 클라우드 프로바이더 조회
- `POST /api/cloud/providers` - 클라우드 프로바이더 생성
- `GET /api/cloud/providers/{providerKey}` - 특정 프로바이더 조회
- `PUT /api/cloud/providers/{providerKey}` - 프로바이더 수정
- `DELETE /api/cloud/providers/{providerKey}` - 프로바이더 삭제
- `GET /api/cloud/providers/active` - 활성 프로바이더 조회
- `GET /api/cloud/providers/global` - 글로벌 프로바이더 조회
- `GET /api/cloud/providers/government` - 정부용 프로바이더 조회
- `GET /api/cloud/providers/type/{providerType}` - 프로바이더 타입별 조회
- `GET /api/cloud/providers/sync-needed` - 동기화가 필요한 프로바이더 조회
- `GET /api/cloud/providers/count/active` - 활성 프로바이더 수 조회
- `PATCH /api/cloud/providers/{providerKey}/activate` - 프로바이더 활성화
- `PATCH /api/cloud/providers/{providerKey}/deactivate` - 프로바이더 비활성화
- `PATCH /api/cloud/providers/{providerKey}/sync` - 프로바이더 동기화 시간 업데이트

### ⚙️ 플랫폼 설정 관리
- `GET /api/platform/configs` - 모든 플랫폼 설정 조회
- `POST /api/platform/configs` - 플랫폼 설정 생성
- `GET /api/platform/configs/{configKey}` - 특정 설정 조회
- `PUT /api/platform/configs/{configKey}` - 설정 수정
- `DELETE /api/platform/configs/{configKey}` - 설정 삭제
- `GET /api/platform/configs/system` - 시스템 설정 조회
- `GET /api/platform/configs/type/{configType}` - 설정 타입별 조회

### 🚩 기능 플래그 관리
- `GET /api/platform/feature-flags` - 모든 기능 플래그 조회
- `POST /api/platform/feature-flags` - 기능 플래그 생성
- `GET /api/platform/feature-flags/{flagKey}` - 특정 플래그 조회
- `PUT /api/platform/feature-flags/{flagKey}` - 플래그 수정
- `DELETE /api/platform/feature-flags/{flagKey}` - 플래그 삭제
- `GET /api/platform/feature-flags/active` - 활성 플래그 조회
- `GET /api/platform/feature-flags/{flagKey}/enabled` - 플래그 활성화 상태 확인
- `PATCH /api/platform/feature-flags/{flagKey}/toggle` - 플래그 토글

## 🐳 Docker 명령어

### 하이브리드 모드 (개발용) - MySQL + phpMyAdmin만 Docker
```bash
# 개발 환경 시작 (MySQL + phpMyAdmin)
docker-compose -f docker-compose.dev.yml up -d

# 개발 환경 중지
docker-compose -f docker-compose.dev.yml down

# 개발 환경 상태 확인
docker-compose -f docker-compose.dev.yml ps

# 개발 환경 로그 확인
docker-compose -f docker-compose.dev.yml logs -f mysql
docker-compose -f docker-compose.dev.yml logs -f phpmyadmin

# 개발 환경 볼륨까지 삭제 (데이터 초기화)
docker-compose -f docker-compose.dev.yml down -v
```

### 전체 Docker 모드 (프로덕션용)
```bash
# 전체 서비스 시작
docker-compose up -d

# 빌드와 함께 시작 (소스코드 변경 후)
docker-compose up --build -d

# 특정 서비스만 시작
docker-compose up -d mysql

# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v

# 로그 확인
docker-compose logs -f [service-name]

# 서비스 재시작
docker-compose restart [service-name]
```

### 개발용 명령어
```bash
# 소스코드 변경 후 재빌드 (전체 Docker 모드)
docker-compose up --build -d

# 특정 서비스만 재빌드
docker-compose up --build -d app

# 서비스 상태 확인
docker-compose ps

# 실시간 로그 모니터링
docker-compose logs -f app

# 컨테이너 내부 접속
docker-compose exec app bash
docker-compose exec mysql mysql -u agenticcp -p agenticcp
```

### 문제 해결 명령어
```bash
# 강제 재빌드 (캐시 무시)
docker-compose build --no-cache
docker-compose up -d

# 특정 서비스만 강제 재빌드
docker-compose build --no-cache app
docker-compose up -d app

# 모든 컨테이너 및 이미지 정리
docker-compose down --rmi all --volumes --remove-orphans

# 로그 정리
docker-compose logs --tail=100 -f app
```

## 🔧 개발 도구

### Maven 명령어

```bash
# 의존성 다운로드
mvn dependency:resolve

# 애플리케이션 실행
mvn spring-boot:run

# 테스트 실행
mvn test

# 패키지 빌드
mvn clean package

# Docker 이미지 빌드
mvn dockerfile:build
```

### 프로파일 설정

- `local`: 로컬 개발 환경 (기본값)
- `docker`: Docker 환경
- `prod`: 프로덕션 환경

## 📁 프로젝트 구조

```
AgenticCP-Core/
├── src/
│   ├── main/
│   │   ├── java/com/agenticcp/core/
│   │   │   ├── AgenticCpCoreApplication.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── docs/
│   ├── DEVELOPMENT_STANDARDS.md
│   ├── CODE_STYLE_GUIDE.md
│   ├── API_DESIGN_GUIDELINES.md
│   └── TESTING_GUIDELINES.md
├── docker/
│   └── mysql/init/
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## 📚 개발 표준 문서

프로젝트의 일관성 있는 개발을 위한 표준 문서들:

- **[개발 표준](./docs/DEVELOPMENT_STANDARDS.md)** - 전체적인 개발 표준 및 가이드라인
- **[코드 스타일 가이드](./docs/CODE_STYLE_GUIDE.md)** - Java, Spring Boot 코드 작성 스타일
- **[API 설계 가이드라인](./docs/API_DESIGN_GUIDELINES.md)** - REST API 설계 및 구현 표준
- **[테스트 가이드라인](./docs/TESTING_GUIDELINES.md)** - 단위/통합 테스트 작성 표준

### 주요 개발 표준 요약

#### 🏗️ 프로젝트 구조
- 패키지 구조: `com.agenticcp.core.{controller|service|repository|entity|dto|exception|util|common}`
- 네이밍 규칙: PascalCase (클래스), camelCase (메서드/변수), snake_case (테이블)

#### 🎨 코드 스타일
- 생성자 주입 사용 (필드 주입 금지)
- `@Transactional(readOnly = true)` 기본 사용
- 적절한 주석 및 JavaDoc 작성
- Lombok 활용 (Getter, Setter, Builder 등)

#### 🗄️ JPA 사용
- `FetchType.LAZY` 기본 사용
- `@CreationTimestamp`, `@UpdateTimestamp` 활용
- 복합 인덱스는 `@Index` 어노테이션으로 명시
- 검증 어노테이션을 엔티티에 적용

#### 🌐 API 설계
- RESTful 원칙 준수
- 리소스 중심 URL 설계
- 적절한 HTTP 상태 코드 사용
- 요청/응답 DTO 분리

#### 🌍 크로스 도메인 처리
- 환경별 CORS 설정
- `@CrossOrigin` 어노테이션 활용
- 보안을 고려한 Origin 제한

#### 🧪 테스트
- Given-When-Then 패턴 사용
- Mock을 활용한 단위 테스트
- TestContainers를 활용한 통합 테스트
- 테스트 커버리지 80% 이상 유지

## 🚨 문제 해결

### 일반적인 문제

1. **포트 충돌**: 8080, 3306, 8081 포트가 사용 중인 경우
   ```bash
   # 포트 사용 확인 (Windows)
   netstat -an | findstr :8080
   
   # 포트 사용 확인 (macOS/Linux)
   lsof -i :8080
   
   # docker-compose.yml에서 포트 변경
   # ports: "8081:8080"  # 호스트:컨테이너
   ```

2. **MySQL 연결 실패**: 컨테이너가 완전히 시작될 때까지 대기
   ```bash
   # MySQL 로그 확인
   docker-compose logs mysql
   
   # MySQL 헬스체크 확인
   docker-compose ps
   
   # MySQL 컨테이너 내부 접속
   docker-compose exec mysql mysql -u root -p
   ```

3. **애플리케이션 시작 실패**: 로그 확인 및 재빌드
   ```bash
   # 애플리케이션 로그 확인
   docker-compose logs app
   
   # 강제 재빌드
   docker-compose up --build --force-recreate -d
   
   # 컨테이너 내부 접속하여 디버깅
   docker-compose exec app bash
   ```

4. **권한 문제**: Docker 볼륨 권한 설정
   ```bash
   # 볼륨 권한 수정 (macOS/Linux)
   sudo chown -R $USER:$USER ./logs
   
   # Docker 데몬 재시작 (필요시)
   sudo systemctl restart docker
   ```

5. **메모리 부족**: Docker 리소스 제한 확인
   ```bash
   # Docker 리소스 사용량 확인
   docker stats
   
   # 사용하지 않는 컨테이너 정리
   docker system prune -a
   ```

### 개발 환경 문제

1. **소스코드 변경이 반영되지 않음**
   ```bash
   # 재빌드 필요
   docker-compose up --build -d
   
   # 또는 특정 서비스만 재빌드
   docker-compose up --build -d app
   ```

2. **데이터베이스 스키마 변경**
   ```bash
   # 볼륨 삭제 후 재시작 (데이터 초기화)
   docker-compose down -v
   docker-compose up -d
   ```

3. **캐시 문제**
   ```bash
   # Maven 캐시 정리
   mvn clean
   
   # Docker 캐시 무시하고 재빌드
   docker-compose build --no-cache
   docker-compose up -d
   ```

## 🚀 빠른 시작 가이드

### 새로운 개발자를 위한 체크리스트

1. **환경 준비**
   - [ ] Java 17 설치 확인
   - [ ] Maven 3.6+ 설치 확인
   - [ ] Docker & Docker Compose 설치 확인
   - [ ] Git 설치 확인

2. **프로젝트 설정**
   ```bash
   # 저장소 클론
   git clone <repository-url>
   cd AgenticCP-Core
   ```

3. **개발 환경 시작 (권장 방법)**
   ```bash
   # 자동 스크립트 사용 (가장 간단)
   # macOS/Linux
   ./start-dev.sh
   
   # Windows
   start-dev.bat
   ```

4. **서비스 확인**
   - [ ] 애플리케이션: http://localhost:8080/api
   - [ ] Swagger UI: http://localhost:8080/api/swagger-ui/index.html
   - [ ] phpMyAdmin: http://localhost:8081
   - [ ] MySQL: localhost:3306

5. **개발 시작**
   ```bash
   # 하이브리드 모드: 소스코드 수정 후 자동 반영
   # (Spring Boot DevTools가 자동으로 재시작)
   
   # 전체 Docker 모드: 소스코드 수정 후
   docker-compose up --build -d
   ```

### 팀 협업 가이드

1. **브랜치 전략**
   ```bash
   # 기능 개발
   git checkout -b feature/새로운기능
   
   # 버그 수정
   git checkout -b bugfix/버그수정
   
   # 핫픽스
   git checkout -b hotfix/긴급수정
   ```

2. **코드 리뷰**
   - PR 생성 전 로컬에서 테스트
   - Docker 환경에서 동작 확인
   - API 문서 업데이트 확인

3. **배포 프로세스**
   ```bash
   # 개발 환경
   docker-compose up --build -d
   
   # 스테이징 환경 (예정)
   docker-compose -f docker-compose.staging.yml up -d
   
   # 프로덕션 환경 (예정)
   docker-compose -f docker-compose.prod.yml up -d
   ```

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
