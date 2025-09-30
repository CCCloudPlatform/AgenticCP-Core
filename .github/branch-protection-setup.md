# 브랜치 보호 규칙 설정 가이드

## 개요
이 문서는 GitHub에서 develop 브랜치에 대한 보호 규칙을 설정하는 방법을 설명합니다.

## 설정 방법

### 1. GitHub 리포지토리 설정 페이지 접근
1. GitHub 리포지토리 페이지로 이동
2. `Settings` 탭 클릭
3. 좌측 메뉴에서 `Branches` 클릭

### 2. 브랜치 보호 규칙 추가
1. "Add rule" 버튼 클릭
2. Branch name pattern에 `develop` 입력

### 3. 보호 규칙 설정

#### Required status checks
- ✅ **Require status checks to pass before merging** 체크
- ✅ **Require branches to be up to date before merging** 체크
- Status checks 목록에서 다음 항목들을 선택:
  - `test` (CI Pipeline의 테스트 잡)
  - `build` (CI Pipeline의 빌드 잡)

#### Additional restrictions
- ✅ **Require pull request reviews before merging** 체크
  - Required number of reviewers: `1`
  - ✅ **Dismiss stale reviews when new commits are pushed** 체크
  - ✅ **Require review from code owners** 체크 (선택사항)

- ✅ **Require conversation resolution before merging** 체크

- ✅ **Require signed commits** 체크 (선택사항)

- ✅ **Require linear history** 체크 (선택사항)

#### Administrative settings
- ✅ **Include administrators** 체크 (관리자도 규칙 적용)
- ✅ **Allow force pushes** 체크 해제
- ✅ **Allow deletions** 체크 해제

### 4. 규칙 저장
"Create" 버튼을 클릭하여 규칙을 저장합니다.

## 결과

이 설정이 완료되면:
- develop 브랜치로의 직접 푸시가 차단됩니다
- 모든 변경사항은 PR을 통해서만 가능합니다
- PR 머지 전에 CI 테스트가 반드시 통과해야 합니다
- 최소 1명의 리뷰어 승인이 필요합니다
- 모든 대화가 해결되어야 합니다

## 참고사항

### CI Status Checks 확인
첫 번째 PR이 생성되고 CI가 실행된 후에야 status checks 목록에 `test`와 `build`가 나타납니다. 
따라서 다음 순서로 진행하는 것을 권장합니다:

1. 현재 PR을 생성하고 CI 실행 확인
2. CI 실행 후 브랜치 보호 규칙 설정
3. Status checks에 CI 잡들을 추가

### 예외 상황
긴급한 상황에서는 관리자가 임시로 규칙을 비활성화하고 머지할 수 있지만, 
이후 반드시 규칙을 다시 활성화해야 합니다.
