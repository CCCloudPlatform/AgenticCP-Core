# 브랜치 보호 규칙 설정 가이드

## 📋 개요

GitHub에서 브랜치 보호 규칙을 설정하여 모든 Pull Request가 테스트를 통과해야만 머지될 수 있도록 합니다.

## 🔧 설정 방법

### 1. GitHub 저장소 설정 접근

1. GitHub 저장소 페이지로 이동
2. **Settings** 탭 클릭
3. 왼쪽 메뉴에서 **Branches** 클릭

### 2. 브랜치 보호 규칙 추가

1. **Add rule** 버튼 클릭
2. **Branch name pattern**에 `develop` 입력
3. 다음 옵션들을 활성화:

#### ✅ 필수 설정

- **Require a pull request before merging**
  - **Require approvals**: 1 (또는 팀 정책에 따라)
  - **Dismiss stale PR approvals when new commits are pushed**: ✅
  - **Require review from code owners**: ✅ (코드 오너가 있는 경우)

- **Require status checks to pass before merging**
  - **Require branches to be up to date before merging**: ✅
  - **Status checks**에서 다음 체크박스 활성화:
    - `Test and Build` (CI/CD 파이프라인의 test job)
    - `Code Quality Check` (CI/CD 파이프라인의 code-quality job)
    - `Security Scan` (CI/CD 파이프라인의 security-scan job)

- **Require conversation resolution before merging**: ✅

- **Require signed commits**: ✅ (선택사항)

- **Require linear history**: ✅ (선택사항)

- **Include administrators**: ✅ (관리자도 규칙 적용)

- **Allow force pushes**: ❌ (비활성화)

- **Allow deletions**: ❌ (비활성화)

### 3. main 브랜치에도 동일한 규칙 적용

1. **Add rule** 버튼으로 새 규칙 추가
2. **Branch name pattern**에 `main` 입력
3. develop 브랜치와 동일한 설정 적용

## 🚀 예상 결과

설정 완료 후:

1. **develop** 또는 **main** 브랜치로의 모든 PR은 다음을 통과해야 함:
   - 코드 리뷰 승인
   - 모든 테스트 통과
   - 코드 품질 검사 통과
   - 보안 스캔 통과

2. **테스트 실패 시 머지 차단**:
   - CI/CD 파이프라인에서 테스트가 실패하면 PR 머지 불가
   - 빨간색 ❌ 표시로 상태 확인 가능

3. **코드 품질 보장**:
   - Checkstyle, SpotBugs, OWASP 검사 통과 필수
   - 테스트 커버리지 리포트 생성

## 📊 모니터링

### GitHub Actions 탭에서 확인 가능:

1. **Actions** 탭 클릭
2. 각 워크플로우 실행 상태 확인:
   - ✅ 성공 (녹색)
   - ❌ 실패 (빨간색)
   - 🟡 진행 중 (노란색)

### PR 페이지에서 확인 가능:

1. PR 페이지 하단의 **Checks** 섹션
2. 각 단계별 실행 결과 확인
3. 실패한 단계 클릭하여 상세 로그 확인

## 🔧 문제 해결

### 테스트 실패 시:

1. **Actions** 탭에서 실패한 워크플로우 클릭
2. 실패한 단계의 로그 확인
3. 로컬에서 동일한 명령어 실행하여 문제 재현
4. 수정 후 다시 푸시

### 코드 품질 검사 실패 시:

1. Checkstyle 오류 수정
2. SpotBugs 경고 해결
3. OWASP 보안 취약점 수정

## 📝 추가 권장사항

1. **코드 리뷰어 지정**: 팀원들을 코드 리뷰어로 지정
2. **자동 머지 설정**: 모든 체크 통과 시 자동 머지 활성화 (선택사항)
3. **브랜치 정책 문서화**: 팀 내 브랜치 정책 문서 작성
4. **정기적인 규칙 검토**: 팀 정책에 따라 규칙 주기적 검토

## 🎯 성공 지표

- 모든 PR이 테스트 통과 후 머지
- 코드 품질 지표 개선
- 보안 취약점 조기 발견
- 개발 워크플로우 자동화
