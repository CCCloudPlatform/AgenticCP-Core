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
mvn spring-boot:run -Dspring-boot.run.profiles=local
