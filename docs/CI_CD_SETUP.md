# CI/CD 파이프라인 설정 가이드

## 개요

이 문서는 AgenticCP-Core 프로젝트의 CI/CD 파이프라인 구성에 대해 설명합니다.

## 🔧 구성된 기능

### 1. GitHub Actions 워크플로우

#### `.github/workflows/ci.yml`
- **트리거**: `develop`, `main` 브랜치로의 push 및 Pull Request
- **실행 환경**: Ubuntu Latest
- **Java 버전**: 17 (Eclipse Temurin)
- **두 단계 파이프라인**: Test → Build

### 2. 테스트 단계 (test job)

#### 서비스 종속성
- **MySQL 8.0.33**: CI 환경에서의 통합 테스트용
- **Redis 7.0**: 캐시 및 세션 관리용

#### 주요 작업
- 소스 체크아웃
- JDK 17 설정
- Maven 의존성 캐싱
- MySQL/Redis 연결 확인
- 테스트 실행 (`mvn clean test`)
- 테스트 리포트 생성
- 테스트 결과 아티팩트 업로드

#### 환경 변수
```yaml
SPRING_PROFILES_ACTIVE: test
SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/agenticcp_test
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: root
SPRING_REDIS_HOST: 127.0.0.1
SPRING_REDIS_PORT: 6379
```

### 3. 빌드 단계 (build job)

#### 주요 작업
- 테스트 단계 성공 후 실행
- 애플리케이션 컴파일
- JAR 패키징
- 빌드 아티팩트 업로드

## 📋 로컬 테스트 설정

### 테스트용 프로파일 (`application-test.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

#### 특징
- **H2 인메모리 데이터베이스**: 빠른 테스트 실행
- **create-drop**: 테스트마다 스키마 재생성
- **Redis 설정**: 로컬 환경 대응

### Maven 설정 개선사항

#### JaCoCo 테스트 커버리지
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <configuration>
        <rules>
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.50</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

#### Surefire 플러그인 업그레이드
- **버전**: 3.0.0
- **XML 리포트**: GitHub Actions 통합
- **테스트 패턴**: `**/*Test.java`, `**/*Tests.java`

#### 의존성 업데이트
- **MySQL Connector**: `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`
- **H2 Database**: 테스트 스코프 추가

## 🚀 CI/CD 파이프라인 실행

### 자동 실행 조건
1. `develop` 또는 `main` 브랜치로 Push
2. `develop` 또는 `main` 브랜치로의 Pull Request

### 실행 단계
1. **Test Job**
   - 환경 설정 (Java 17, MySQL, Redis)
   - 의존성 설치
   - 테스트 실행
   - 커버리지 리포트 생성

2. **Build Job** (테스트 성공 시)
   - 애플리케이션 빌드
   - JAR 패키징
   - 아티팩트 저장

### 결과 확인
- **GitHub Actions 탭**: 워크플로우 실행 상태
- **Artifacts**: 테스트 리포트 및 빌드 결과물
- **Checks**: PR에서 상태 확인

## 📝 브랜치 보호 규칙 설정

### 권장 설정 (`.github/branch-protection-setup.md` 참조)

1. **Required status checks**
   - `test` (CI Pipeline의 테스트 잡)
   - `build` (CI Pipeline의 빌드 잡)

2. **Pull Request 요구사항**
   - 최소 1명의 리뷰어 승인
   - 대화 해결 필수
   - 최신 상태 유지

3. **관리자 규칙 적용**
   - 관리자도 동일한 규칙 적용
   - Force push 차단
   - 브랜치 삭제 차단

## 🔍 테스트 커버리지

### 현재 상태
- **최소 커버리지**: 패키지당 50%
- **리포트 위치**: `target/site/jacoco/`
- **GitHub Actions**: 자동 아티팩트 업로드

### 개선 방안
1. 커버리지 목표 단계적 증가
2. 중요 비즈니스 로직 우선 커버
3. 통합 테스트 추가

## 🚨 문제 해결

### 일반적인 문제

#### 1. 테스트 실패
```bash
mvn clean test -Dspring.profiles.active=test
```

#### 2. 의존성 문제
```bash
mvn dependency:resolve
mvn clean compile
```

#### 3. 데이터베이스 연결 문제
- 로컬: H2 인메모리 사용
- CI: MySQL/Redis 서비스 확인

### 로그 확인
- **GitHub Actions**: 워크플로우 로그
- **로컬**: `target/surefire-reports/`
- **커버리지**: `target/site/jacoco/index.html`

## 📈 향후 계획

### Phase 1 (완료)
- ✅ GitHub Actions 워크플로우 구축
- ✅ 테스트 자동화
- ✅ 커버리지 리포팅
- ✅ 브랜치 보호 규칙 가이드

### Phase 2 (예정)
- [ ] SonarQube 코드 품질 분석
- [ ] Docker 이미지 빌드 자동화
- [ ] 배포 파이프라인 구축
- [ ] 보안 스캔 통합

### Phase 3 (예정)
- [ ] 성능 테스트 자동화
- [ ] 다중 환경 배포
- [ ] 모니터링 통합
- [ ] 롤백 자동화

## 📞 지원

문제가 발생하거나 개선 사항이 있으면 다음을 통해 문의하세요:

1. **GitHub Issues**: 버그 리포트 및 기능 요청
2. **Pull Request**: 개선 사항 제안
3. **Documentation**: 이 문서 업데이트

---

**마지막 업데이트**: 2025-09-30  
**작성자**: AgenticCP Team
