package com.agenticcp.core.domain.ui.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.logging.LogMaskingUtils;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.exception.MenuErrorCode;
import com.agenticcp.core.domain.ui.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 메뉴 초기화 서비스
 * 테넌트 생성 시 기본 메뉴 구조를 생성
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MenuInitializationService {

    private final MenuRepository menuRepository;
    private final MenuService menuService;

    /**
     * 테넌트별 기본 메뉴 구조 초기화
     * 
     * @param tenant 테넌트
     * @return 생성된 메뉴 목록
     */
    public List<Menu> initializeDefaultMenus(Tenant tenant) {
        log.info("[MenuInitializationService] initializeDefaultMenus - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));

        try {
            // 기존 메뉴 존재 확인
            if (!menuRepository.findByTenantAndActive(tenant).isEmpty()) {
                log.warn("[MenuInitializationService] Menus already exist for tenant: {}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
                return menuRepository.findByTenantAndActive(tenant);
            }

            List<Menu> createdMenus = createDefaultMenuStructure(tenant);
            
            log.info("[MenuInitializationService] initializeDefaultMenus - success tenantKey={} count={}", 
                    LogMaskingUtils.maskTenantKey(tenant.getTenantKey()), createdMenus.size());
            
            return createdMenus;

        } catch (Exception e) {
            log.error("[MenuInitializationService] Failed to initialize menus for tenant: {} error: {}", 
                    LogMaskingUtils.maskTenantKey(tenant.getTenantKey()), e.getMessage(), e);
            throw new BusinessException(MenuErrorCode.MENU_INITIALIZATION_FAILED, 
                    "메뉴 초기화에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 기본 메뉴 구조 생성
     * 
     * @param tenant 테넌트
     * @return 생성된 메뉴 목록
     */
    private List<Menu> createDefaultMenuStructure(Tenant tenant) {
        log.info("[MenuInitializationService] createDefaultMenuStructure - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));

        // 1. 대시보드 (최상위 메뉴)
        Menu dashboard = menuService.createMenu(
                "DASHBOARD",
                "대시보드",
                "플랫폼 대시보드",
                "/dashboard",
                "dashboard",
                null,
                1,
                true
        );

        // 2. 클라우드 관리 (최상위 메뉴)
        Menu cloudManagement = menuService.createMenu(
                "CLOUD_MANAGEMENT",
                "클라우드 관리",
                "클라우드 리소스 관리",
                "/cloud",
                "cloud",
                null,
                2,
                true
        );

        // 3. 사용자 관리 (최상위 메뉴)
        Menu userManagement = menuService.createMenu(
                "USER_MANAGEMENT",
                "사용자 관리",
                "사용자 및 권한 관리",
                "/users",
                "users",
                null,
                3,
                true
        );

        // 4. 모니터링 (최상위 메뉴)
        Menu monitoring = menuService.createMenu(
                "MONITORING",
                "모니터링",
                "시스템 모니터링 및 알림",
                "/monitoring",
                "monitoring",
                null,
                4,
                true
        );

        // 5. 비용 관리 (최상위 메뉴)
        Menu costManagement = menuService.createMenu(
                "COST_MANAGEMENT",
                "비용 관리",
                "클라우드 비용 관리",
                "/cost",
                "cost",
                null,
                5,
                true
        );

        // 하위 메뉴 생성
        createCloudManagementSubMenus(cloudManagement);
        createUserManagementSubMenus(userManagement);
        createMonitoringSubMenus(monitoring);
        createCostManagementSubMenus(costManagement);

        return Arrays.asList(dashboard, cloudManagement, userManagement, monitoring, costManagement);
    }

    /**
     * 클라우드 관리 하위 메뉴 생성
     * 
     * @param parentMenu 부모 메뉴
     */
    private void createCloudManagementSubMenus(Menu parentMenu) {
        menuService.createMenu("AWS_ACCOUNTS", "AWS 계정", "AWS 계정 관리", "/cloud/aws/accounts", "aws", parentMenu.getId(), 1, true);
        menuService.createMenu("AZURE_ACCOUNTS", "Azure 계정", "Azure 계정 관리", "/cloud/azure/accounts", "azure", parentMenu.getId(), 2, true);
        menuService.createMenu("GCP_ACCOUNTS", "GCP 계정", "GCP 계정 관리", "/cloud/gcp/accounts", "gcp", parentMenu.getId(), 3, true);
        menuService.createMenu("RESOURCES", "리소스 관리", "클라우드 리소스 관리", "/cloud/resources", "resources", parentMenu.getId(), 4, true);
    }

    /**
     * 사용자 관리 하위 메뉴 생성
     * 
     * @param parentMenu 부모 메뉴
     */
    private void createUserManagementSubMenus(Menu parentMenu) {
        menuService.createMenu("USERS", "사용자", "사용자 관리", "/users/list", "user", parentMenu.getId(), 1, true);
        menuService.createMenu("ROLES", "역할", "역할 관리", "/users/roles", "role", parentMenu.getId(), 2, true);
        menuService.createMenu("PERMISSIONS", "권한", "권한 관리", "/users/permissions", "permission", parentMenu.getId(), 3, true);
        menuService.createMenu("ORGANIZATIONS", "조직", "조직 관리", "/users/organizations", "organization", parentMenu.getId(), 4, true);
    }

    /**
     * 모니터링 하위 메뉴 생성
     * 
     * @param parentMenu 부모 메뉴
     */
    private void createMonitoringSubMenus(Menu parentMenu) {
        menuService.createMenu("DASHBOARDS", "대시보드", "모니터링 대시보드", "/monitoring/dashboards", "dashboard", parentMenu.getId(), 1, true);
        menuService.createMenu("ALERTS", "알림", "알림 관리", "/monitoring/alerts", "alert", parentMenu.getId(), 2, true);
        menuService.createMenu("LOGS", "로그", "시스템 로그", "/monitoring/logs", "log", parentMenu.getId(), 3, true);
        menuService.createMenu("METRICS", "메트릭", "성능 메트릭", "/monitoring/metrics", "metric", parentMenu.getId(), 4, true);
    }

    /**
     * 비용 관리 하위 메뉴 생성
     * 
     * @param parentMenu 부모 메뉴
     */
    private void createCostManagementSubMenus(Menu parentMenu) {
        menuService.createMenu("COST_ANALYSIS", "비용 분석", "비용 분석 및 리포트", "/cost/analysis", "analysis", parentMenu.getId(), 1, true);
        menuService.createMenu("BUDGETS", "예산", "예산 관리", "/cost/budgets", "budget", parentMenu.getId(), 2, true);
        menuService.createMenu("BILLING", "청구서", "청구서 관리", "/cost/billing", "billing", parentMenu.getId(), 3, true);
        menuService.createMenu("OPTIMIZATION", "최적화", "비용 최적화", "/cost/optimization", "optimization", parentMenu.getId(), 4, true);
    }

    /**
     * 시스템 메뉴 존재 여부 확인
     * 
     * @param tenant 테넌트
     * @return 시스템 메뉴 존재 여부
     */
    public boolean hasSystemMenus(Tenant tenant) {
        List<Menu> systemMenus = menuRepository.findSystemMenusByTenant(tenant);
        return !systemMenus.isEmpty();
    }

    /**
     * 특정 메뉴 키의 시스템 메뉴 존재 여부 확인
     * 
     * @param tenant 테넌트
     * @param menuKey 메뉴 키
     * @return 메뉴 존재 여부
     */
    public boolean hasSystemMenuByKey(Tenant tenant, String menuKey) {
        return menuRepository.findByMenuKeyAndTenant(menuKey, tenant)
                .map(Menu::getIsSystem)
                .orElse(false);
    }
}
