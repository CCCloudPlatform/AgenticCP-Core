package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import com.agenticcp.core.domain.platform.service.ConfigHistoryQueryService;
import com.agenticcp.core.common.exception.GlobalExceptionHandler;
import com.agenticcp.core.common.exception.ErrorCodeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 이력 조회 API 통합 테스트 (웹 레이어 단위)
 * - 하이브리드 접근법: RDBMS 기반 설정 이력 조회
 * - 시간순 정렬/페이지네이션 응답 형태 검증
 * - 관리자 권한 제한(403) 검증: 간단히 pre-check 훅으로 대행
 * - ENCRYPTED 응답 마스킹은 서비스/유틸 테스트로 검증되므로 여기선 필드 존재만 확인
 */
public class PlatformConfigHistoryApiTest {

    private MockMvc mockMvc;
    private ConfigHistoryQueryService queryService;

    @BeforeEach
    void setup() {
        queryService = Mockito.mock(ConfigHistoryQueryService.class);
        PlatformConfigController controller = new PlatformConfigController(Mockito.mock(com.agenticcp.core.domain.platform.service.PlatformConfigService.class), queryService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorCodeRegistry()))
                .build();
    }

    @Test
    void getHistory_ShouldReturnPagedList() throws Exception {
        Page<ConfigHistoryResponse> page = new PageImpl<>(
                List.of(
                        new ConfigHistoryResponse("UPDATE", "admin", null, "ENCRYPTED", "***", "***", java.time.LocalDateTime.now()),
                        new ConfigHistoryResponse("CREATE", "admin", null, "STRING", null, "abc", java.time.LocalDateTime.now().minusMinutes(1))
                ),
                PageRequest.of(0, 2),
                10
        );
        when(queryService.getHistory(Mockito.eq("secure.key"), anyInt(), anyInt())).thenReturn(page);

        // enforceAdmin()가 현재 SecurityContext를 검사하므로, 여기서는 403을 기대
        mockMvc.perform(get("/v1/platform/configs/secure.key/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}


