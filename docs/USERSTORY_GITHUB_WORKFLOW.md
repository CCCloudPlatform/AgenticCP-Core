# UserStory 기반 GitHub 이슈 관리 워크플로우

## 📋 개요

AgenticCP 프로젝트의 UserStory 기반 개발을 위한 GitHub 이슈 관리 워크플로우 가이드입니다.

## 🏗️ 이슈 계층 구조

### 4단계 계층 구조
```
Epic (대형 기능 영역)
├── UserStory (사용자 관점의 기능)
│   ├── Feature (기술적 구현 단위)
│   │   ├── Task (개발 작업)
│   │   └── Task (개발 작업)
│   └── Feature (기술적 구현 단위)
└── UserStory (사용자 관점의 기능)
```

### 각 단계별 역할

| 단계 | 역할 | 책임 | 라벨 | 예시 |
|------|------|------|------|------|
| **Epic** | 비즈니스 목표 정의 | 큰 기능 영역 정의 | `epic` | "AWS 통합 관리" |
| **UserStory** | 사용자 요구사항 | 사용자 관점 기능 정의 | `user-story` | "AWS 계정 등록 기능" |
| **Feature** | 기술적 구현 | UserStory 구현 단위 | `feature` | "AWS 계정 등록 API" |
| **Task** | 개발 작업 | 구체적 개발 작업 | `task` | "AWS SDK 의존성 추가" |

## 🚀 개발 워크플로우

### 1. Epic 생성 및 계획

#### Epic 생성
1. **GitHub 이슈 생성**: `[EPIC]` 템플릿 사용
2. **비즈니스 목표 정의**: Epic이 달성하고자 하는 목표 명시
3. **UserStory 목록 작성**: 포함될 UserStory들 나열
4. **라벨 설정**: `epic`, `needs-triage`
5. **담당자 할당**: 프로젝트 매니저 또는 팀 리드

#### Epic 예시
```yaml
title: [EPIC] AWS 통합 관리 시스템
labels: [epic, needs-triage]
assignees: [project-manager]

비즈니스 목표: AWS 리소스를 통합적으로 관리할 수 있는 시스템 구축
포함 UserStory:
- AWS 계정 등록 및 인증
- AWS 리소스 조회 및 관리
- AWS 비용 모니터링
```

### 2. UserStory 생성 및 분석

#### UserStory 생성
1. **GitHub 이슈 생성**: `[USER-STORY]` 템플릿 사용
2. **Epic 연결**: 상위 Epic 링크
3. **사용자 관점 작성**: "As a [사용자], I want [기능], So that [목적]"
4. **수용 기준 정의**: 명확한 완료 기준 설정
5. **Story Point 추정**: Fibonacci 수열 사용 (1, 2, 3, 5, 8, 13, 21)
6. **라벨 설정**: `user-story`, `needs-triage`

#### UserStory 예시
```yaml
title: [USER-STORY] AWS 계정 등록 기능
labels: [user-story, needs-triage]
assignees: [developer]

Epic: #123 (AWS 통합 관리 시스템)

UserStory:
As a 사용자
I want AWS 계정을 등록할 수 있도록
So that AWS 리소스를 관리할 수 있다

수용 기준:
- AWS 계정 정보 입력 폼 제공
- AWS 인증 정보 암호화 저장
- AWS 연결 테스트 기능
- 등록 완료 후 대시보드 이동

Story Point: 5 SP
```

### 3. Feature 생성 및 설계

#### Feature 생성
1. **GitHub 이슈 생성**: `[FEATURE]` 템플릿 사용
2. **UserStory 연결**: 상위 UserStory 링크
3. **기술적 구현 계획**: API, 데이터 모델, 기술 스택 정의
4. **Task 목록 작성**: 구현할 Task들 나열
5. **라벨 설정**: `feature`, `needs-triage`

#### Feature 예시
```yaml
title: [FEATURE] AWS 계정 등록 API
labels: [feature, needs-triage]
assignees: [backend-developer]

UserStory: #456 (AWS 계정 등록 기능)

기능 요구사항:
- POST /api/aws/accounts - AWS 계정 등록
- GET /api/aws/accounts - 등록된 계정 목록 조회
- POST /api/aws/accounts/{id}/test - 연결 테스트

포함 Task:
- AWS SDK 의존성 추가
- AWS 계정 엔티티 설계
- AWS 계정 등록 서비스 구현
- AWS 계정 등록 API 구현
- AWS 연결 테스트 기능 구현
```

### 4. Task 생성 및 개발

#### Task 생성
1. **GitHub 이슈 생성**: `[TASK]` 템플릿 사용
2. **Feature 연결**: 상위 Feature 링크
3. **구체적 작업 정의**: 구현할 파일, 클래스, 메서드 명시
4. **개발자 할당**: 실제 개발 담당자
5. **라벨 설정**: `task`, `needs-triage`

#### Task 예시
```yaml
title: [TASK] AWS 계정 등록 서비스 구현
labels: [task, needs-triage]
assignees: [developer]

Feature: #789 (AWS 계정 등록 API)

구체적 작업:
- AwsAccountService 클래스 구현
- AWS 인증 정보 암호화 로직
- AWS 연결 테스트 메서드
- 예외 처리 및 로깅

구현할 파일:
- src/main/java/.../service/AwsAccountService.java
- src/test/java/.../service/AwsAccountServiceTest.java
```

## 🔄 이슈 상태 관리

### 이슈 상태 라벨

| 라벨 | 설명 | 사용 시점 |
|------|------|-----------|
| `needs-triage` | 검토 필요 | 이슈 생성 시 |
| `ready-for-work` | 작업 준비 완료 | 분석 완료 후 |
| `in-progress` | 작업 진행 중 | 개발 시작 시 |
| `in-review` | 리뷰 중 | PR 생성 시 |
| `done` | 완료 | 작업 완료 시 |
| `blocked` | 차단됨 | 의존성 이슈 발생 시 |
| `cancelled` | 취소됨 | 작업 취소 시 |

### 상태 전환 워크플로우

```
needs-triage → ready-for-work → in-progress → in-review → done
     ↓              ↓              ↓
  cancelled      blocked        blocked
```

## 📊 스프린트 관리

### 스프린트 계획 (Sprint Planning)

#### 1. Epic 우선순위 설정
- 비즈니스 가치 기준으로 Epic 우선순위 결정
- `epic` 라벨로 Epic 이슈 필터링
- 우선순위별로 Epic 정렬

#### 2. UserStory 선택
- Epic별로 UserStory 목록 확인
- Story Point 합계가 팀 용량 내에서 선택
- `user-story` 라벨로 UserStory 이슈 필터링

#### 3. Feature 및 Task 분해
- 선택된 UserStory의 Feature 확인
- Feature별로 Task 생성 및 할당
- 개발자별 작업 분배

### 일일 스탠드업

#### 진행 상황 공유
- 어제 완료한 Task (몇 SP 완료)
- 오늘 계획된 Task (몇 SP 예정)
- 블로커 및 이슈

#### 이슈 상태 업데이트
- 완료된 Task: `done` 라벨 추가
- 진행 중인 Task: `in-progress` 라벨 유지
- 차단된 Task: `blocked` 라벨 추가

### 스프린트 리뷰

#### 완료된 기능 시연
- `done` 라벨이 있는 Task들 확인
- 완료된 UserStory 기능 시연
- 이해관계자 피드백 수집

#### 다음 스프린트 계획
- 미완료 Task 다음 스프린트로 이동
- 새로운 UserStory 우선순위 조정
- 팀 벨로시티 분석

## 🏷️ 라벨 체계

### 라벨 카테고리

#### 이슈 타입
- `epic`: Epic 이슈
- `user-story`: UserStory 이슈
- `feature`: Feature 이슈
- `task`: Task 이슈

#### 상태
- `needs-triage`: 검토 필요
- `ready-for-work`: 작업 준비 완료
- `in-progress`: 작업 진행 중
- `in-review`: 리뷰 중
- `done`: 완료
- `blocked`: 차단됨
- `cancelled`: 취소됨

#### 우선순위
- `priority-high`: 높음
- `priority-medium`: 중간
- `priority-low`: 낮음

#### 도메인
- `domain-auth`: 인증/인가
- `domain-cloud`: 클라우드 관리
- `domain-monitoring`: 모니터링
- `domain-cost`: 비용 관리
- `domain-ui`: 사용자 인터페이스

#### 기술 스택
- `tech-backend`: 백엔드
- `tech-frontend`: 프론트엔드
- `tech-database`: 데이터베이스
- `tech-aws`: AWS
- `tech-azure`: Azure
- `tech-gcp`: GCP

## 🔍 이슈 검색 및 필터링

### GitHub 이슈 검색 쿼리

#### Epic별 UserStory 조회
```
label:user-story label:needs-triage
```

#### 진행 중인 Task 조회
```
label:task label:in-progress
```

#### 완료된 Feature 조회
```
label:feature label:done
```

#### 특정 도메인 이슈 조회
```
label:domain-cloud label:in-progress
```

#### 높은 우선순위 이슈 조회
```
label:priority-high -label:done
```

### 이슈 보드 설정

#### 칸반 보드 구성
```
To Do | In Progress | In Review | Done
------|-------------|-----------|-----
needs-triage | in-progress | in-review | done
ready-for-work | blocked | | cancelled
```

#### 필터 설정
- Epic별 보드: `epic` 라벨 필터
- UserStory별 보드: `user-story` 라벨 필터
- Feature별 보드: `feature` 라벨 필터
- Task별 보드: `task` 라벨 필터

## 📈 진행 상황 추적

### 백로그 버닝 차트

#### Story Point 추적
- X축: 스프린트 일수
- Y축: 남은 Story Point
- 이상선: 계획된 완료 라인
- 실제선: 실제 완료 라인

#### 분석 방법
- 실제선이 이상선 위에 있으면: 지연 위험
- 실제선이 이상선 아래에 있으면: 앞서 진행
- 급격한 변화: 스코프 변경 또는 추정 오류

### 벨로시티 측정

#### 팀 벨로시티 계산
- 완료된 Story Point 합계
- 스프린트별 벨로시티 기록
- 평균 벨로시티 계산

#### 활용 방법
- 다음 스프린트 용량 계획
- 프로젝트 완료 일정 예측
- 팀 성능 개선 추적

## 🚀 GitHub Actions 자동화

### 이슈 자동 라벨링

#### Epic 생성 시
```yaml
name: Auto-label Epic
on:
  issues:
    types: [opened]
jobs:
  label-epic:
    if: contains(github.event.issue.title, '[EPIC]')
    runs-on: ubuntu-latest
    steps:
      - name: Add epic label
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.addLabels({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              labels: ['epic', 'needs-triage']
            })
```

#### Task 완료 시 상위 이슈 업데이트
```yaml
name: Update Parent Issue
on:
  issues:
    types: [closed]
jobs:
  update-parent:
    if: contains(github.event.issue.labels.*.name, 'task')
    runs-on: ubuntu-latest
    steps:
      - name: Check if all tasks are done
        uses: actions/github-script@v6
        with:
          script: |
            // 상위 Feature의 모든 Task가 완료되었는지 확인
            // 완료되었으면 Feature를 done으로 변경
```

### 이슈 템플릿 자동 적용

#### Epic 생성 시
- 자동으로 `epic` 라벨 추가
- 프로젝트 매니저에게 자동 할당
- `needs-triage` 상태로 설정

#### UserStory 생성 시
- 자동으로 `user-story` 라벨 추가
- `needs-triage` 상태로 설정
- Epic 연결 확인

## 📋 체크리스트

### Epic 생성 시
- [ ] 비즈니스 목표 명확히 정의
- [ ] 포함할 UserStory 목록 작성
- [ ] 성공 기준 설정
- [ ] 우선순위 결정
- [ ] 담당자 할당

### UserStory 생성 시
- [ ] 사용자 관점으로 작성
- [ ] 수용 기준 명확히 정의
- [ ] Story Point 추정
- [ ] 상위 Epic 연결
- [ ] 우선순위 설정

### Feature 생성 시
- [ ] 기술적 구현 계획 수립
- [ ] API 엔드포인트 정의
- [ ] 데이터 모델 설계
- [ ] 포함할 Task 목록 작성
- [ ] 상위 UserStory 연결

### Task 생성 시
- [ ] 구체적 작업 내용 정의
- [ ] 구현할 파일/클래스 명시
- [ ] 예상 작업 시간 추정
- [ ] 상위 Feature 연결
- [ ] 개발자 할당

### 스프린트 계획 시
- [ ] Epic 우선순위 설정
- [ ] UserStory 선택 및 할당
- [ ] Feature 및 Task 분해
- [ ] 개발자별 작업 분배
- [ ] 스프린트 목표 설정

### 일일 스탠드업 시
- [ ] 어제 완료 작업 공유
- [ ] 오늘 계획 작업 공유
- [ ] 블로커 및 이슈 공유
- [ ] 이슈 상태 업데이트

### 스프린트 리뷰 시
- [ ] 완료된 기능 시연
- [ ] 이해관계자 피드백 수집
- [ ] 다음 스프린트 계획
- [ ] 팀 벨로시티 분석

이 워크플로우를 통해 체계적이고 효율적인 UserStory 기반 개발이 가능합니다.
