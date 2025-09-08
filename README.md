# AgenticCP-Core

CloudPlatform 2.0 Core Application - Spring Boot 기반의 마이크로서비스 애플리케이션

## 🚀 기술 스택

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MySQL 8.0
- **Container**: Docker, Docker Compose
- **Build Tool**: Maven
- **ORM**: Spring Data JPA, Hibernate

## 📋 주요 기능

- 사용자 관리 (CRUD)
- RESTful API
- 데이터베이스 연동
- Docker 컨테이너화
- 헬스 체크 엔드포인트

## 🛠️ 개발 환경 설정

### 사전 요구사항

- Java 17 이상
- Maven 3.6 이상
- Docker & Docker Compose
- Git

### 로컬 개발 환경 실행

1. **저장소 클론**
   ```bash
   git clone <repository-url>
   cd AgenticCP-Core
   ```

2. **Docker 환경으로 실행**
   ```bash
   # 모든 서비스 시작 (MySQL, 애플리케이션, phpMyAdmin)
   docker-compose up -d
   
   # 로그 확인
   docker-compose logs -f app
   ```

3. **개별 서비스 실행**
   ```bash
   # MySQL만 실행
   docker-compose up -d mysql
   
   # 로컬에서 Spring Boot 애플리케이션 실행
   mvn spring-boot:run
   ```

### 접속 정보

- **애플리케이션**: http://localhost:8080
- **API 문서**: http://localhost:8080/api/actuator
- **phpMyAdmin**: http://localhost:8081
- **MySQL**: localhost:3306

### 데이터베이스 정보

- **데이터베이스명**: agenticcp
- **사용자명**: agenticcp
- **비밀번호**: agenticcppassword
- **Root 비밀번호**: rootpassword

## 📚 API 엔드포인트

### 사용자 관리

- `GET /api/users` - 모든 사용자 조회
- `GET /api/users/active` - 활성 사용자 조회
- `GET /api/users/{id}` - 특정 사용자 조회
- `GET /api/users/username/{username}` - 사용자명으로 조회
- `GET /api/users/email/{email}` - 이메일로 조회
- `GET /api/users/search?name={name}` - 이름으로 검색
- `POST /api/users` - 사용자 생성
- `PUT /api/users/{id}` - 사용자 수정
- `DELETE /api/users/{id}` - 사용자 삭제
- `PATCH /api/users/{id}/deactivate` - 사용자 비활성화

### 헬스 체크

- `GET /api/health` - 애플리케이션 상태 확인
- `GET /api/health/ready` - 준비 상태 확인

## 🐳 Docker 명령어

```bash
# 전체 서비스 시작
docker-compose up -d

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
   # 포트 사용 확인
   netstat -an | findstr :8080
   
   # docker-compose.yml에서 포트 변경
   ```

2. **MySQL 연결 실패**: 컨테이너가 완전히 시작될 때까지 대기
   ```bash
   # MySQL 로그 확인
   docker-compose logs mysql
   ```

3. **권한 문제**: Docker 볼륨 권한 설정
   ```bash
   # 볼륨 권한 수정
   sudo chown -R $USER:$USER ./logs
   ```

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
