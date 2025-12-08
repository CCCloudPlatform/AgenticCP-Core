package com.agenticcp.core.common.interceptor;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 테넌트 인식 Hibernate Interceptor
 * 
 * <p>
 * Hibernate의 Interceptor와 StatementInspector를 구현하여 모든 SQL 쿼리를 가로채고,
 * 자동으로 tenant_id 조건을 추가하여 멀티 테넌시 환경에서 데이터 격리를 보장합니다.
 * </p>
 * 
 * <p>
 * 주요 기능:
 * - SELECT 쿼리: WHERE 절에 tenant_id 필터 자동 추가
 * - UPDATE 쿼리: WHERE 절에 tenant_id 필터 자동 추가
 * - DELETE 쿼리: WHERE 절에 tenant_id 필터 자동 추가
 * - INSERT 쿼리: tenant_id 컬럼과 값 자동 주입
 * </p>
 * 
 * <p>
 * 이 Interceptor는 Repository 계층의 테넌트 필터링을 보완하여 
 * 네이티브 쿼리나 직접 SQL 실행 시에도 데이터 격리를 보장합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 * @see org.hibernate.Interceptor
 * @see org.hibernate.resource.jdbc.spi.StatementInspector
 * @see com.agenticcp.core.common.context.TenantContextHolder
 */
@Slf4j
@Component
public class TenantAwareInterceptor implements Interceptor, StatementInspector {

    /**
     * SELECT 쿼리 패턴 매칭을 위한 정규식
     * 예: SELECT * FROM users
     */
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "(?i)\\bSELECT\\b.*?\\bFROM\\b\\s+(\\w+)", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    /**
     * UPDATE 쿼리 패턴 매칭을 위한 정규식
     * 예: UPDATE users SET name = 'John'
     */
    private static final Pattern UPDATE_PATTERN = Pattern.compile(
        "(?i)\\bUPDATE\\b\\s+(\\w+)\\s+\\bSET\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * DELETE 쿼리 패턴 매칭을 위한 정규식
     * 예: DELETE FROM users
     */
    private static final Pattern DELETE_PATTERN = Pattern.compile(
        "(?i)\\bDELETE\\b\\s+\\bFROM\\b\\s+(\\w+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * INSERT 쿼리 패턴 매칭을 위한 정규식
     * 예: INSERT INTO users (name, email)
     */
    private static final Pattern INSERT_PATTERN = Pattern.compile(
        "(?i)\\bINSERT\\b\\s+\\bINTO\\b\\s+(\\w+)\\s*\\(", 
        Pattern.CASE_INSENSITIVE
    );

    /**
     * SQL 쿼리를 검사하고 테넌트 필터링을 적용합니다.
     * 
     * <p>
     * Hibernate가 SQL을 실행하기 직전에 호출되어 모든 쿼리에 테넌트 컨텍스트를 적용합니다.
     * TenantContextHolder에서 현재 테넌트 정보를 가져와 SQL 쿼리에 자동으로 주입합니다.
     * </p>
     * 
     * <p>
     * 처리 흐름:
     * 1. SQL이 null이거나 비어있으면 그대로 반환
     * 2. 테넌트 컨텍스트 존재 여부 확인
     * 3. 현재 테넌트 키 조회
     * 4. SQL 타입(SELECT/UPDATE/DELETE/INSERT)에 따라 처리
     * 5. 수정된 SQL 반환
     * </p>
     * 
     * @param sql 원본 SQL 쿼리
     * @return 테넌트 필터링이 적용된 SQL 쿼리
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않았거나 SQL 처리 중 오류 발생 시
     */
    @Override
    public String inspect(String sql) {
        // null 또는 빈 SQL은 그대로 반환
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            // 현재 테넌트 컨텍스트 확인
            if (!TenantContextHolder.hasTenantContext()) {
                log.warn("테넌트 컨텍스트 없음 - SQL 실행 허용: sql={}", sql.substring(0, Math.min(50, sql.length())));
                return sql;
            }

            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.debug("SQL 인터셉트 시작: tenantKey={}, sqlType={}", 
                tenantKey, getSqlType(sql));

            // SQL 타입에 따라 테넌트 필터링 적용
            String modifiedSql = modifySqlForTenant(sql, tenantKey);
            
            // SQL이 수정되었으면 로그 기록
            if (!sql.equals(modifiedSql)) {
                log.debug("SQL 수정 완료: tenantKey={}, original={}, modified={}", 
                    tenantKey, 
                    sql.substring(0, Math.min(50, sql.length())),
                    modifiedSql.substring(0, Math.min(50, modifiedSql.length())));
            }
            
            return modifiedSql;
            
        } catch (BusinessException e) {
            // BusinessException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("SQL 인터셉트 중 오류 발생: sql={}, error={}", 
                sql.substring(0, Math.min(50, sql.length())), e.getMessage(), e);
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "데이터베이스 작업을 위해 테넌트 컨텍스트가 필요합니다");
        }
    }

    /**
     * SQL 쿼리를 테넌트에 맞게 수정합니다.
     * 
     * <p>
     * SQL 타입(SELECT/UPDATE/DELETE/INSERT)을 판단하여 
     * 각각에 맞는 테넌트 필터링 로직을 적용합니다.
     * </p>
     * 
     * @param sql 원본 SQL 쿼리
     * @param tenantKey 현재 테넌트 키
     * @return 테넌트 필터링이 적용된 수정된 SQL 쿼리
     */
    private String modifySqlForTenant(String sql, String tenantKey) {
        String trimmedSql = sql.trim();
        
        // SELECT 쿼리 처리 - WHERE 절에 tenant_id 필터 추가
        if (trimmedSql.toUpperCase().startsWith("SELECT")) {
            return addTenantFilterToSelect(trimmedSql, tenantKey);
        }
        
        // UPDATE 쿼리 처리 - WHERE 절에 tenant_id 필터 추가
        if (trimmedSql.toUpperCase().startsWith("UPDATE")) {
            return addTenantFilterToUpdate(trimmedSql, tenantKey);
        }
        
        // DELETE 쿼리 처리 - WHERE 절에 tenant_id 필터 추가
        if (trimmedSql.toUpperCase().startsWith("DELETE")) {
            return addTenantFilterToDelete(trimmedSql, tenantKey);
        }
        
        // INSERT 쿼리 처리 - tenant_id 컬럼과 값 주입
        if (trimmedSql.toUpperCase().startsWith("INSERT")) {
            return addTenantToInsert(trimmedSql, tenantKey);
        }
        
        // 알 수 없는 SQL 타입은 그대로 반환
        return sql;
    }

    /**
     * SELECT 쿼리에 tenant_id 필터를 추가합니다.
     * 
     * <p>
     * 정규식을 사용하여 테이블명을 추출하고, WHERE 절에 tenant_id 조건을 추가합니다.
     * 기존 WHERE 절이 있으면 AND로 연결하고, 없으면 새로 생성합니다.
     * </p>
     * 
     * <p>
     * 변환 예시:
     * - 원본: SELECT * FROM users WHERE age > 18
     * - 결과: SELECT * FROM users WHERE users.tenant_id = 'tenant1' AND age > 18
     * </p>
     * 
     * @param sql 원본 SELECT 쿼리
     * @param tenantKey 현재 테넌트 키
     * @return tenant_id 필터가 추가된 SELECT 쿼리
     */
    private String addTenantFilterToSelect(String sql, String tenantKey) {
        Matcher matcher = SELECT_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // WHERE 절이 이미 있는지 확인
            if (sql.toUpperCase().contains("WHERE")) {
                // 기존 WHERE 절에 tenant_id 조건을 맨 앞에 추가 (AND로 연결)
                return sql.replaceFirst("(?i)\\bWHERE\\b", 
                    "WHERE " + tableName + ".tenant_id = '" + tenantKey + "' AND ");
            } else {
                // WHERE 절이 없으면 새로 추가
                return sql + " WHERE " + tableName + ".tenant_id = '" + tenantKey + "'";
            }
        }
        return sql;
    }

    /**
     * UPDATE 쿼리에 tenant_id 필터를 추가합니다.
     * 
     * <p>
     * 정규식을 사용하여 테이블명을 추출하고, WHERE 절에 tenant_id 조건을 추가합니다.
     * 이를 통해 다른 테넌트의 데이터가 실수로 수정되는 것을 방지합니다.
     * </p>
     * 
     * <p>
     * 변환 예시:
     * - 원본: UPDATE users SET name = 'John' WHERE id = 1
     * - 결과: UPDATE users SET name = 'John' WHERE users.tenant_id = 'tenant1' AND id = 1
     * </p>
     * 
     * @param sql 원본 UPDATE 쿼리
     * @param tenantKey 현재 테넌트 키
     * @return tenant_id 필터가 추가된 UPDATE 쿼리
     */
    private String addTenantFilterToUpdate(String sql, String tenantKey) {
        Matcher matcher = UPDATE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // WHERE 절이 이미 있는지 확인
            if (sql.toUpperCase().contains("WHERE")) {
                // 기존 WHERE 절에 tenant_id 조건을 맨 앞에 추가 (AND로 연결)
                return sql.replaceFirst("(?i)\\bWHERE\\b", 
                    "WHERE " + tableName + ".tenant_id = '" + tenantKey + "' AND ");
            } else {
                // WHERE 절이 없으면 새로 추가
                return sql + " WHERE " + tableName + ".tenant_id = '" + tenantKey + "'";
            }
        }
        return sql;
    }

    /**
     * DELETE 쿼리에 tenant_id 필터를 추가합니다.
     * 
     * <p>
     * 정규식을 사용하여 테이블명을 추출하고, WHERE 절에 tenant_id 조건을 추가합니다.
     * 이를 통해 다른 테넌트의 데이터가 실수로 삭제되는 것을 방지합니다.
     * </p>
     * 
     * <p>
     * 변환 예시:
     * - 원본: DELETE FROM users WHERE id = 1
     * - 결과: DELETE FROM users WHERE users.tenant_id = 'tenant1' AND id = 1
     * </p>
     * 
     * @param sql 원본 DELETE 쿼리
     * @param tenantKey 현재 테넌트 키
     * @return tenant_id 필터가 추가된 DELETE 쿼리
     */
    private String addTenantFilterToDelete(String sql, String tenantKey) {
        Matcher matcher = DELETE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // WHERE 절이 이미 있는지 확인
            if (sql.toUpperCase().contains("WHERE")) {
                // 기존 WHERE 절에 tenant_id 조건을 맨 앞에 추가 (AND로 연결)
                return sql.replaceFirst("(?i)\\bWHERE\\b", 
                    "WHERE " + tableName + ".tenant_id = '" + tenantKey + "' AND ");
            } else {
                // WHERE 절이 없으면 새로 추가
                return sql + " WHERE " + tableName + ".tenant_id = '" + tenantKey + "'";
            }
        }
        return sql;
    }

    /**
     * INSERT 쿼리에 tenant_id 컬럼과 값을 자동으로 주입합니다.
     * 
     * <p>
     * 정규식을 사용하여 테이블명과 컬럼 리스트를 추출하고, 
     * tenant_id 컬럼을 맨 앞에 추가하며, VALUES 절에도 테넌트 키를 주입합니다.
     * </p>
     * 
     * <p>
     * 변환 예시:
     * - 원본: INSERT INTO users (name, email) VALUES ('John', 'john@example.com')
     * - 결과: INSERT INTO users (tenant_id, name, email) VALUES ('tenant1', 'John', 'john@example.com')
     * </p>
     * 
     * @param sql 원본 INSERT 쿼리
     * @param tenantKey 현재 테넌트 키
     * @return tenant_id가 주입된 INSERT 쿼리
     */
    private String addTenantToInsert(String sql, String tenantKey) {
        Matcher matcher = INSERT_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // INSERT INTO table (columns) VALUES (values) 형태에서
            // 1. columns 리스트 맨 앞에 tenant_id 추가
            // 2. VALUES 절의 값 리스트 맨 앞에 tenant_key 추가
            return sql.replaceFirst("(?i)\\bINSERT\\b\\s+\\bINTO\\b\\s+" + tableName + "\\s*\\(", 
                "INSERT INTO " + tableName + " (tenant_id, ")
                .replaceFirst("(?i)\\bVALUES\\b\\s*\\(", 
                    "VALUES ('" + tenantKey + "', ");
        }
        return sql;
    }

    /**
     * SQL 타입을 문자열로 반환합니다 (로깅용 헬퍼 메서드).
     * 
     * @param sql SQL 쿼리
     * @return SQL 타입 (SELECT, INSERT, UPDATE, DELETE, UNKNOWN)
     */
    private String getSqlType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "EMPTY";
        }
        
        String trimmedSql = sql.trim().toUpperCase();
        if (trimmedSql.startsWith("SELECT")) {
            return "SELECT";
        } else if (trimmedSql.startsWith("INSERT")) {
            return "INSERT";
        } else if (trimmedSql.startsWith("UPDATE")) {
            return "UPDATE";
        } else if (trimmedSql.startsWith("DELETE")) {
            return "DELETE";
        } else {
            return "UNKNOWN";
        }
    }

    // ========== Hibernate Interceptor 인터페이스 기본 구현 ==========

    /**
     * 엔티티가 데이터베이스에서 로드될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없으므로 false를 반환합니다.
     * </p>
     * 
     * @param entity 로드된 엔티티
     * @param id 엔티티 ID
     * @param state 엔티티 상태 배열
     * @param propertyNames 속성 이름 배열
     * @param types 속성 타입 배열
     * @return 상태가 변경되었으면 true, 아니면 false
     */
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        return false;
    }

    /**
     * 엔티티가 더티(dirty) 상태로 감지되어 업데이트될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없으므로 false를 반환합니다.
     * </p>
     * 
     * @param entity 업데이트될 엔티티
     * @param id 엔티티 ID
     * @param currentState 현재 상태 배열
     * @param previousState 이전 상태 배열
     * @param propertyNames 속성 이름 배열
     * @param types 속성 타입 배열
     * @return 상태가 변경되었으면 true, 아니면 false
     */
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, org.hibernate.type.Type[] types) {
        return false;
    }

    /**
     * 엔티티가 저장될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없으므로 false를 반환합니다.
     * tenant_id는 SQL 레벨에서 자동으로 주입됩니다.
     * </p>
     * 
     * @param entity 저장될 엔티티
     * @param id 엔티티 ID
     * @param state 엔티티 상태 배열
     * @param propertyNames 속성 이름 배열
     * @param types 속성 타입 배열
     * @return 상태가 변경되었으면 true, 아니면 false
     */
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        return false;
    }

    /**
     * 엔티티가 삭제될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없습니다.
     * tenant_id 필터링은 SQL 레벨에서 자동으로 적용됩니다.
     * </p>
     * 
     * @param entity 삭제될 엔티티
     * @param id 엔티티 ID
     * @param state 엔티티 상태 배열
     * @param propertyNames 속성 이름 배열
     * @param types 속성 타입 배열
     */
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        // tenant_id 필터링은 SQL 레벨에서 처리됨
    }

    /**
     * 컬렉션이 삭제될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없습니다.
     * </p>
     * 
     * @param collection 삭제될 컬렉션
     * @param key 컬렉션 키
     */
    @Override
    public void onCollectionRemove(Object collection, Serializable key) {
        // 현재 추가 처리 없음
    }

    /**
     * 컬렉션이 재생성될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없습니다.
     * </p>
     * 
     * @param collection 재생성될 컬렉션
     * @param key 컬렉션 키
     */
    @Override
    public void onCollectionRecreate(Object collection, Serializable key) {
        // 현재 추가 처리 없음
    }

    /**
     * 컬렉션이 업데이트될 때 호출됩니다.
     * 
     * <p>
     * 현재 구현에서는 추가 처리가 필요 없습니다.
     * </p>
     * 
     * @param collection 업데이트될 컬렉션
     * @param key 컬렉션 키
     */
    @Override
    public void onCollectionUpdate(Object collection, Serializable key) {
        // 현재 추가 처리 없음
    }
}
