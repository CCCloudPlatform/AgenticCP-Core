# 브랜치 보호 규칙 설정 가이드

## 📋 개요
이 문서는 GitHub 저장소에서 브랜치 보호 규칙을 설정하는 방법을 설명합니다.

## 🎯 목표
- develop 및 main 브랜치에 대한 보호 규칙 설정
- 테스트 통과 필수 조건 적용
- 코드 품질 보장

## 🔧 설정 방법

### 1. GitHub 저장소 설정 접근
1. GitHub 저장소 페이지로 이동
2. **Settings** 탭 클릭
3. 왼쪽 메뉴에서 **Branches** 클릭

### 2. 브랜치 보호 규칙 추가

#### develop 브랜치 보호 규칙
1. **Add rule** 버튼 클릭
2. **Branch name pattern**에 `develop` 입력
3. 다음 옵션들을 활성화:
   - ✅ **Require a pull request before merging**
     - ✅ **Require approvals** (1명 이상)
     - ✅ **Dismiss stale PR approvals when new commits are pushed**
     - ✅ **Require review from code owners**
   - ✅ **Require status checks to pass before merging**
     - ✅ **Require branches to be up to date before merging**
     - Status checks에서 다음 항목들을 선택:
       - `Test Suite`
       - `Build Application`
       - `Code Quality Check`
       - `Security Scan`
   - ✅ **Require conversation resolution before merging**
   - ✅ **Require signed commits**
   - ✅ **Require linear history**
   - ✅ **Include administrators** (관리자도 규칙 적용)

#### main 브랜치 보호 규칙
1. **Add rule** 버튼 클릭
2. **Branch name pattern**에 `main` 입력
3. develop 브랜치와 동일한 설정 적용
4. 추가로 다음 옵션 활성화:
   - ✅ **Restrict pushes that create files** (파일 생성 제한)
   - ✅ **Restrict pushes that create files larger than 100MB**

### 3. CODEOWNERS 파일 생성 (선택사항)
`.github/CODEOWNERS` 파일을 생성하여 코드 리뷰어를 지정할 수 있습니다:

```
# Global code owners
* @CCCloudPlatform/developers

# Specific file patterns
*.java @CCCloudPlatform/java-team
*.yml @CCCloudPlatform/devops-team
*.md @CCCloudPlatform/docs-team

# Directory specific
/src/main/java/ @CCCloudPlatform/backend-team
/.github/workflows/ @CCCloudPlatform/devops-team
```

## 🚀 설정 완료 후 확인사항

### 1. PR 생성 테스트
1. 새로운 브랜치에서 변경사항 커밋
2. develop 브랜치로 PR 생성
3. 다음 사항들 확인:
   - ✅ CI/CD 파이프라인이 자동 실행
   - ✅ 모든 테스트가 통과해야만 머지 가능
   - ✅ 코드 리뷰 승인 필요
   - ✅ 대화 해결 필요

### 2. 브랜치 보호 규칙 확인
- develop 브랜치에 직접 푸시 시도 → 차단되어야 함
- 테스트 실패 시 머지 시도 → 차단되어야 함
- 승인 없는 PR 머지 시도 → 차단되어야 함

## 📊 모니터링 및 관리

### 1. 상태 확인
- **Actions** 탭에서 CI/CD 파이프라인 상태 확인
- **Insights** → **Pulse**에서 저장소 활동 모니터링
- **Settings** → **Branches**에서 보호 규칙 상태 확인

### 2. 규칙 업데이트
- 필요에 따라 보호 규칙 수정
- 새로운 상태 체크 추가/제거
- 승인자 수 조정

## 🔧 문제 해결

### 일반적인 문제
1. **CI/CD 파이프라인이 실행되지 않음**
   - `.github/workflows/ci.yml` 파일 확인
   - YAML 문법 오류 확인
   - 권한 설정 확인

2. **테스트가 실패하지만 머지가 가능함**
   - 브랜치 보호 규칙에서 상태 체크 활성화 확인
   - 상태 체크 이름이 정확한지 확인

3. **관리자도 규칙이 적용됨**
   - "Include administrators" 옵션 비활성화
   - 또는 관리자 권한으로 우회

### 권한 문제
- 저장소 관리자 권한이 필요
- 조직 저장소의 경우 조직 설정 확인
- 팀 권한 설정 확인

## 📚 참고 자료
- [GitHub 브랜치 보호 규칙 문서](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [CODEOWNERS 파일 가이드](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)
