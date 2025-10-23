package com.agenticcp.core.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 기본 Repository 인터페이스 - 테넌트 격리 없음
 * 전역 기능(플랫폼 설정, 클라우드 제공자 등)에서 사용
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // 기본 JPA 메서드만 제공
    // 테넌트 필터링 없이 모든 데이터에 접근
}
