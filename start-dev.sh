#!/bin/bash

# AgenticCP-Core 개발 환경 시작 스크립트

echo "🚀 AgenticCP-Core 개발 환경을 시작합니다..."

# MySQL과 phpMyAdmin만 시작
echo "📦 MySQL과 phpMyAdmin을 시작합니다..."

# docker compose 명령어 탐지 (v2: docker compose, v1: docker-compose)
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    DC_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    DC_CMD="docker-compose"
else
    echo "❌ docker compose/docker-compose 가 설치되어 있지 않습니다."
    echo "   Docker Desktop 설치 또는 docker compose(v2) 활성화 후 다시 실행하세요."
    exit 1
fi

$DC_CMD -f docker-compose.dev.yml up -d

# 서비스가 완전히 시작될 때까지 대기
echo "⏳ 서비스가 시작될 때까지 대기 중..."
sleep 10

# 서비스 상태 확인
echo "🔍 서비스 상태를 확인합니다..."
$DC_CMD -f docker-compose.dev.yml ps

# MySQL 연결 확인
echo "🔗 MySQL 연결을 확인합니다..."
until $DC_CMD -f docker-compose.dev.yml exec mysql mysqladmin ping -h localhost --silent; do
    echo "MySQL이 시작될 때까지 대기 중..."
    sleep 2
done

echo "✅ MySQL이 준비되었습니다!"

# Spring Boot 애플리케이션 시작
echo "☕ Spring Boot 애플리케이션을 시작합니다..."
echo "프로파일: local"
echo "데이터베이스: localhost:3306"
echo "애플리케이션: http://localhost:8080/api"
echo "Swagger UI: http://localhost:8080/api/swagger-ui/index.html"
echo "phpMyAdmin: http://localhost:8081"
echo ""
echo "애플리케이션을 중지하려면 Ctrl+C를 누르세요."

# Spring Boot 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local
