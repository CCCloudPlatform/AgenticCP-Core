package com.agenticcp.core.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 기본 Repository 인터페이스 - 테넌트 격리 없음
 * 
 * <p>
 * 전역 기능(플랫폼 설정, 클라우드 제공자, 시스템 설정 등)에서 사용하는 베이스 리포지토리입니다.
 * BaseEntity를 상속받은 엔티티에 대한 데이터 액세스 계층을 제공합니다.
 * </p>
 * 
 * <p>
 * 이 인터페이스는 테넌트 필터링 없이 모든 데이터에 접근하므로, 
 * 멀티 테넌시가 필요한 경우 TenantAwareRepository를 사용해야 합니다.
 * </p>
 * 
 * <p>
 * {@code @NoRepositoryBean} 어노테이션을 통해 Spring Data JPA가 
 * 이 인터페이스의 구현체를 생성하지 않도록 합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @see com.agenticcp.core.common.entity.BaseEntity
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // 기본 JPA 메서드만 제공 (상속된 메서드)
    // - save(): 엔티티 저장
    // - findById(): ID로 엔티티 조회
    // - findAll(): 모든 엔티티 조회
    // - deleteById(): ID로 엔티티 삭제
    // - count(): 엔티티 개수 조회
    // 등 JpaRepository의 모든 CRUD 메서드 사용 가능
    
    // 테넌트 필터링 없이 모든 데이터에 접근
    // 멀티 테넌시 데이터 격리가 필요한 경우 TenantAwareRepository 사용 필요
}
