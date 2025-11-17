@echo off
REM AgenticCP-Core 개발 환경 시작 스크립트 (Windows)

echo 🚀 AgenticCP-Core 개발 환경을 시작합니다...

REM MySQL과 phpMyAdmin만 시작
echo 📦 MySQL과 phpMyAdmin을 시작합니다...
docker-compose -f docker-compose.dev.yml up -d

REM 서비스가 완전히 시작될 때까지 대기
echo ⏳ 서비스가 시작될 때까지 대기 중...
timeout /t 10 /nobreak > nul

REM 서비스 상태 확인
echo 🔍 서비스 상태를 확인합니다...
docker-compose -f docker-compose.dev.yml ps

REM MySQL 연결 확인
echo 🔗 MySQL 연결을 확인합니다...
:wait_mysql
docker-compose -f docker-compose.dev.yml exec mysql mysqladmin ping -h localhost --silent > nul 2>&1
if errorlevel 1 (
    echo MySQL이 시작될 때까지 대기 중...
    timeout /t 2 /nobreak > nul
    goto wait_mysql
)

echo ✅ MySQL이 준비되었습니다!

REM Spring Boot 애플리케이션 시작
echo ☕ Spring Boot 애플리케이션을 시작합니다...
echo 프로파일: local
echo 데이터베이스: localhost:3306
echo 애플리케이션: http://localhost:8080/api
echo Swagger UI: http://localhost:8080/api/swagger-ui/index.html
echo phpMyAdmin: http://localhost:8081
echo.
echo 애플리케이션을 중지하려면 Ctrl+C를 누르세요.

REM Spring Boot 실행
REM 환경변수 설정
echo 🔧 환경변수를 설정합니다...

REM .env 파일이 존재하는지 확인하고 로드
if exist .env (
    echo 📄 .env 파일을 로드합니다...
    for /f "tokens=1,2 delims==" %%a in (.env) do (
        set "%%a=%%b"
    )
) else (
    echo ⚠️  .env 파일이 없습니다. env.example을 참고하여 .env 파일을 생성하세요.
    echo 📝 기본값을 사용합니다 (개발용 - 프로덕션에서는 반드시 .env 파일 사용 필수)
    set DATABASE_URL=jdbc:mysql://localhost:3306/agenticcp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    set DATABASE_USERNAME=agenticcp
    set DATABASE_PASSWORD=agenticcppassword
    if not defined JWT_SECRET set JWT_SECRET=ZmFrZV9zZWNyZXRfZm9yX2Rldl9vbmx5X3VzZV9jaGFuZ2VfbWU=
    if not defined CONFIG_CIPHER_KEY set CONFIG_CIPHER_KEY=MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDA=
    set SPRING_DATA_REDIS_HOST=localhost
    set SPRING_DATA_REDIS_PORT=6379
    set APP_REDIS_ENABLED=false
)

mvn spring-boot:run -Dspring-boot.run.profiles=local
