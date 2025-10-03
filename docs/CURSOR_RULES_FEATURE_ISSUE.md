# Feature 이슈 템플릿

> 이 문서는 GitHub에 Feature 이슈를 생성할 때 사용할 내용입니다.

---

**제목:** `[FEATURE] Cursor Rules 작성 - 팀 개발 표준 자동화`

**라벨:** `feature`, `needs-triage`, `enhancement`, `documentation`

**담당자:** `[담당자 GitHub 아이디]`

---

## 📋 상위 연결
- **UserStory:** #[해당하는 UserStory 번호] (개발 환경 표준화)
- **Epic:** #[해당하는 Epic 번호] (개발 인프라 구축)

## 🎯 기능 요구사항

### 목적
팀원들이 Cursor AI 편집기를 사용할 때 AgenticCP 프로젝트의 개발 표준을 자동으로 적용받아 일관된 코드 스타일로 개발할 수 있도록 `.cursor/rules/` 파일을 작성합니다.

### 배경
- Cursor Rules가 `.cursorrules` 파일에서 `.cursor/rules/` 디렉토리 구조로 업데이트됨
- 프로젝트가 성장하면서 팀원 간 코드 스타일 불일치 발생
- 신규 팀원 온보딩 시 개발 표준 학습에 많은 시간 소요
- AI 기반 개발 도구를 활용하여 생산성 향상 필요

### 주요 기능

#### 1. `.cursor/rules/` 디렉토리 구조 생성
```
.cursor/
└── rules/
    ├── 00-project-overview.md
    ├── 01-code-style.md
    ├── 02-naming-conventions.md
    ├── 03-package-structure.md
    ├── 04-api-design.md
    ├── 05-exception-handling.md
    ├── 06-database-design.md
    ├── 07-testing.md
    ├── 08-logging-security.md
    ├── 09-domain-specific.md
    └── 10-forbidden-practices.md
```

#### 2. 각 규칙 파일 작성
- **프로젝트 개요**: 멀티클라우드 플랫폼, 기술 스택, 아키텍처
- **코드 스타일**: 들여쓰기, 괄호, 주석 규칙
- **네이밍 규칙**: 클래스/메서드/변수/패키지 네이밍
- **패키지 구조**: domain 기반 구조, 계층별 역할
- **API 설계**: RESTful URL, HTTP 메서드, ApiResponse 사용
- **예외 처리**: BusinessException, ErrorCategory, BaseErrorCode 사용
- **데이터베이스**: Entity 설계, Repository 패턴, 인덱스 관리
- **테스트**: Given-When-Then, @Nested, Mock 사용법
- **로깅/보안**: 입력 검증, 로그 레벨 사용
- **도메인 규칙**: 멀티테넌트, 도메인별 에러 코드 범위
- **금지 사항**: 필드 주입, SELECT *, 매직 넘버 등

#### 3. 개발자 가이드 문서 작성
- README에 Cursor AI 사용 방법 추가
- 실제 사용 예시 및 팁 제공
- 트러블슈팅 가이드

### 참고 문서
- `docs/CODE_STYLE_GUIDE.md` - 코드 스타일 가이드
- `docs/DEVELOPMENT_STANDARDS.md` - 개발 표준
- `docs/API_DESIGN_GUIDELINES.md` - API 설계 가이드라인
- `docs/EXCEPTION_GUIDLINES.md` - 예외 처리 가이드라인
- `docs/TESTING_GUIDELINES.md` - 테스트 가이드라인
- `docs/DOMAIN_ARCHITECTURE.md` - 도메인 아키텍처

### 예상 효과
- ✅ **개발 생산성 향상**: AI가 자동으로 표준에 맞는 코드 생성 (20-30% 향상)
- ✅ **코드 일관성 유지**: 모든 팀원이 동일한 스타일로 개발
- ✅ **온보딩 시간 단축**: 신규 팀원 학습 시간 50% 감소
- ✅ **리뷰 시간 단축**: 스타일 관련 리뷰 포인트 최소화 (30% 감소)
- ✅ **반복 작업 최소화**: AI 자동 완성으로 보일러플레이트 코드 자동 생성

## 📝 포함 Task

### Task 1: 기존 가이드라인 분석 및 정리 (4시간)
- [ ] `CODE_STYLE_GUIDE.md` 분석
- [ ] `DEVELOPMENT_STANDARDS.md` 분석
- [ ] `API_DESIGN_GUIDELINES.md` 분석
- [ ] `EXCEPTION_GUIDLINES.md` 분석
- [ ] `TESTING_GUIDELINES.md` 분석
- [ ] `DOMAIN_ARCHITECTURE.md` 분석
- [ ] 핵심 규칙 추출 및 우선순위 결정
- [ ] Cursor AI가 이해하기 쉬운 형식으로 재구성

### Task 2: .cursor/rules/ 디렉토리 및 파일 작성 (8시간)
- [ ] `.cursor/rules/` 디렉토리 생성
- [ ] `00-project-overview.md` 작성 (프로젝트 소개)
- [ ] `01-code-style.md` 작성 (코드 스타일)
- [ ] `02-naming-conventions.md` 작성 (네이밍 규칙)
- [ ] `03-package-structure.md` 작성 (패키지 구조)
- [ ] `04-api-design.md` 작성 (API 설계)
- [ ] `05-exception-handling.md` 작성 (예외 처리)
- [ ] `06-database-design.md` 작성 (데이터베이스)
- [ ] `07-testing.md` 작성 (테스트)
- [ ] `08-logging-security.md` 작성 (로깅/보안)
- [ ] `09-domain-specific.md` 작성 (도메인 특수 규칙)
- [ ] `10-forbidden-practices.md` 작성 (금지 사항)

### Task 3: 검증 및 테스트 (4시간)
- [ ] Cursor AI로 새로운 도메인 생성 테스트 (예: Product 도메인)
- [ ] Entity/Repository/Service/Controller 자동 생성 검증
- [ ] API 엔드포인트 생성 검증
- [ ] DTO 및 예외 처리 코드 생성 검증
- [ ] 테스트 코드 자동 생성 검증
- [ ] 생성된 코드가 가이드라인 준수하는지 확인
- [ ] 문제점 발견 시 규칙 파일 수정

### Task 4: 문서화 및 배포 (2시간)
- [ ] `README.md`에 "Cursor AI 개발 가이드" 섹션 추가
- [ ] 사용 예시 작성 (도메인 추가, API 생성, 테스트 작성)
- [ ] 주의사항 및 FAQ 작성
- [ ] 트러블슈팅 가이드 작성
- [ ] PR 생성 및 팀원 리뷰 요청 (최소 2명)
- [ ] 피드백 반영 및 수정
- [ ] Merge 후 전체 팀원에게 공유
- [ ] 사용 설명 세션 진행 (선택)

## ✅ 완료 조건 (Definition of Done)
- [ ] `.cursor/rules/` 디렉토리가 생성되고 모든 규칙 파일이 작성되어 있음
- [ ] 모든 주요 개발 표준 규칙이 포함되어 있음 (코드 스타일, API, 예외, 테스트 등)
- [ ] 실제 코드 생성 테스트를 통과함 (최소 3개 시나리오)
- [ ] 테스트 결과가 문서화되어 있음 (스크린샷 포함)
- [ ] `README.md`에 Cursor AI 사용 가이드가 추가되어 있음
- [ ] 팀원 2명 이상의 리뷰 승인을 받음
- [ ] develop 브랜치에 Merge 완료
- [ ] 팀 전체에 공유 완료

## 📊 예상 작업 시간
- **분석 및 정리**: 4시간
- **.cursor/rules/ 작성**: 8시간
- **테스트 및 검증**: 4시간
- **문서화**: 2시간
- **리뷰 및 수정**: 2시간

**총 예상 시간: 20시간 (약 2.5일)**

## 🔗 관련 이슈
- **Related to**: #[관련 이슈 번호]
- **Depends on**: #[의존하는 이슈 번호]
- **Blocks**: #[이 이슈가 완료되어야 진행 가능한 이슈]

## 📌 추가 정보

### 기술 스택
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.x
- **데이터베이스**: MySQL 8.0, Redis
- **ORM**: JPA (Hibernate)
- **빌드 도구**: Maven
- **테스트**: JUnit 5, Mockito, TestContainers

### 참고 링크
- [Cursor AI Documentation](https://docs.cursor.sh/)
- [Cursor Rules Guide](https://docs.cursor.sh/context/rules)

---

## 📝 작업 진행 시 업데이트

### 진행 상황
- [ ] 작업 시작
- [ ] Task 1 완료
- [ ] Task 2 완료
- [ ] Task 3 완료
- [ ] Task 4 완료
- [ ] 최종 완료

### 블로커 및 이슈
- 없음

### 참고 사항
- 없음

