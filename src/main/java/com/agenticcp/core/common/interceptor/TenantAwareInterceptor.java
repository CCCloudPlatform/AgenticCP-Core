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
 * 모든 SQL 쿼리에 자동으로 tenant_id 조건을 추가하여 테넌트 데이터 격리 보장
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
public class TenantAwareInterceptor implements Interceptor, StatementInspector {

    // SQL 쿼리 패턴 매칭을 위한 정규식
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "(?i)\\bSELECT\\b.*?\\bFROM\\b\\s+(\\w+)", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern UPDATE_PATTERN = Pattern.compile(
        "(?i)\\bUPDATE\\b\\s+(\\w+)\\s+\\bSET\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DELETE_PATTERN = Pattern.compile(
        "(?i)\\bDELETE\\b\\s+\\bFROM\\b\\s+(\\w+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSERT_PATTERN = Pattern.compile(
        "(?i)\\bINSERT\\b\\s+\\bINTO\\b\\s+(\\w+)\\s*\\(", 
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String inspect(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            // 현재 테넌트 컨텍스트 확인
            if (!TenantContextHolder.hasTenantContext()) {
                log.warn("No tenant context found for SQL: {}", sql);
                return sql;
            }

            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.debug("Intercepting SQL with tenant: {} - {}", tenantKey, sql);

            // SQL 타입에 따라 처리
            String modifiedSql = modifySqlForTenant(sql, tenantKey);
            
            if (!sql.equals(modifiedSql)) {
                log.debug("Modified SQL: {}", modifiedSql);
            }
            
            return modifiedSql;
            
        } catch (Exception e) {
            log.error("Error in tenant-aware SQL interception: {}", e.getMessage(), e);
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "Tenant context is required for database operations");
        }
    }

    /**
     * SQL 쿼리를 테넌트에 맞게 수정
     * 
     * @param sql 원본 SQL 쿼리
     * @param tenantKey 현재 테넌트 키
     * @return 수정된 SQL 쿼리
     */
    private String modifySqlForTenant(String sql, String tenantKey) {
        String trimmedSql = sql.trim();
        
        // SELECT 쿼리 처리
        if (trimmedSql.toUpperCase().startsWith("SELECT")) {
            return addTenantFilterToSelect(trimmedSql, tenantKey);
        }
        
        // UPDATE 쿼리 처리
        if (trimmedSql.toUpperCase().startsWith("UPDATE")) {
            return addTenantFilterToUpdate(trimmedSql, tenantKey);
        }
        
        // DELETE 쿼리 처리
        if (trimmedSql.toUpperCase().startsWith("DELETE")) {
            return addTenantFilterToDelete(trimmedSql, tenantKey);
        }
        
        // INSERT 쿼리 처리
        if (trimmedSql.toUpperCase().startsWith("INSERT")) {
            return addTenantToInsert(trimmedSql, tenantKey);
        }
        
        return sql;
    }

    /**
     * SELECT 쿼리에 tenant_id 필터 추가
     */
    private String addTenantFilterToSelect(String sql, String tenantKey) {
        Matcher matcher = SELECT_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // WHERE 절이 이미 있는지 확인
            if (sql.toUpperCase().contains("WHERE")) {
                // 기존 WHERE 절에 tenant_id 조건 추가
                return sql.replaceFirst("(?i)\\bWHERE\\b", 
                    "WHERE " + tableName + ".tenant_id = '" + tenantKey + "' AND ");
            } else {
                // WHERE 절이 없으면 추가
                return sql + " WHERE " + tableName + ".tenant_id = '" + tenantKey + "'";
            }
        }
        return sql;
    }

    /**
     * UPDATE 쿼리에 tenant_id 필터 추가
     */
    private String addTenantFilterToUpdate(String sql, String tenantKey) {
        Matcher matcher = UPDATE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // WHERE 절이 이미 있는지 확인
            if (sql.toUpperCase().contains("WHERE")) {
                // 기존 WHERE 절에 tenant_id 조건 추가
                return sql.replaceFirst("(?i)\\bWHERE\\b", 
                    "WHERE " + tableName + ".tenant_id = '" + tenantKey + "' AND ");
            } else {
                // WHERE 절이 없으면 추가
                return sql + " WHERE " + tableName + ".tenant_id = '" + tenantKey + "'";
            }
        }
        return sql;
    }

    /**
     * DELETE 쿼리에 tenant_id 필터 추가
     */
    private String addTenantFilterToDelete(String sql, String tenantKey) {
        Matcher matcher = DELETE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // WHERE 절이 이미 있는지 확인
            if (sql.toUpperCase().contains("WHERE")) {
                // 기존 WHERE 절에 tenant_id 조건 추가
                return sql.replaceFirst("(?i)\\bWHERE\\b", 
                    "WHERE " + tableName + ".tenant_id = '" + tenantKey + "' AND ");
            } else {
                // WHERE 절이 없으면 추가
                return sql + " WHERE " + tableName + ".tenant_id = '" + tenantKey + "'";
            }
        }
        return sql;
    }

    /**
     * INSERT 쿼리에 tenant_id 자동 주입
     */
    private String addTenantToInsert(String sql, String tenantKey) {
        Matcher matcher = INSERT_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            
            // INSERT INTO table (columns) VALUES (values) 형태에서
            // columns에 tenant_id 추가하고 values에 tenant_key 추가
            return sql.replaceFirst("(?i)\\bINSERT\\b\\s+\\bINTO\\b\\s+" + tableName + "\\s*\\(", 
                "INSERT INTO " + tableName + " (tenant_id, ")
                .replaceFirst("(?i)\\bVALUES\\b\\s*\\(", 
                    "VALUES ('" + tenantKey + "', ");
        }
        return sql;
    }

    // Interceptor 인터페이스의 기본 구현들
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, org.hibernate.type.Type[] types) {
        return false;
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        return false;
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        // 삭제 시 추가 로직이 필요한 경우 구현
    }

    @Override
    public void onCollectionRemove(Object collection, Serializable key) {
        // 컬렉션 삭제 시 추가 로직이 필요한 경우 구현
    }

    @Override
    public void onCollectionRecreate(Object collection, Serializable key) {
        // 컬렉션 재생성 시 추가 로직이 필요한 경우 구현
    }

    @Override
    public void onCollectionUpdate(Object collection, Serializable key) {
        // 컬렉션 업데이트 시 추가 로직이 필요한 경우 구현
    }
}
