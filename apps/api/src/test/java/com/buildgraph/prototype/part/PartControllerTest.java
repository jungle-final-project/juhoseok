package com.buildgraph.prototype.part;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buildgraph.prototype.user.CurrentUserService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(PartController.class)
class PartControllerTest {
    private static final String USER_TOKEN = "Bearer jwt-user-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartQueryService partQueryService;

    @MockitoBean
    private ToolCheckService toolCheckService;

    @MockitoBean
    private NaverShoppingOfferService naverShoppingOfferService;

    @MockitoBean
    private PartCompatibleCandidateService compatibleCandidateService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUpAuth() {
        when(currentUserService.requireUser(null))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    @Test
    void toolCheckReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/tools/power/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

        verifyNoInteractions(toolCheckService);
    }

    @Test
    void toolCheckRunsForAuthenticatedUserToken() throws Exception {
        when(toolCheckService.checkTool(eq("power"), anyMap())).thenReturn(Map.of(
                "tool", "power",
                "status", "PASS",
                "confidence", "HIGH",
                "summary", "전력 검증 통과",
                "details", Map.of("ratedHeadroomW", 180)
        ));

        mockMvc.perform(post("/api/tools/power/check")
                        .header("Authorization", USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool").value("power"))
                .andExpect(jsonPath("$.status").value("PASS"))
                .andExpect(jsonPath("$.confidence").value("HIGH"));

        verify(currentUserService).requireUser(USER_TOKEN);
        verify(toolCheckService).checkTool(eq("power"), anyMap());
    }

    @Test
    void compatibleCandidatesRequireUserToken() throws Exception {
        mockMvc.perform(post("/api/parts/compatible-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(compatibleCandidateService);
    }

    @Test
    void compatibleCandidatesReturnServerCheckedOptions() throws Exception {
        CurrentUserService.CurrentUser user = new CurrentUserService.CurrentUser(
                1004L,
                "00000000-0000-4000-8000-000000001004",
                "user@example.com",
                "Demo User",
                "USER",
                "2026-06-30T00:00:00Z"
        );
        when(currentUserService.requireUser(USER_TOKEN)).thenReturn(user);
        when(compatibleCandidateService.compatibleCandidates(eq(user), anyMap())).thenReturn(Map.of(
                "category", "GPU",
                "items", java.util.List.of(Map.of(
                        "part", Map.of("id", "part-gpu-pass", "category", "GPU", "name", "RTX 5070 Ti", "price", 990000),
                        "status", "PASS",
                        "statusLabel", "여유 있음",
                        "summary", "현재 PSU/케이스 기준 장착 가능합니다.",
                        "checkedTools", java.util.List.of("power", "size")
                )),
                "rejectedCount", 1,
                "warnings", java.util.List.of()
        ));

        mockMvc.perform(post("/api/parts/compatible-candidates")
                        .header("Authorization", USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "AI_BUILD",
                                  "category": "GPU",
                                  "items": [{ "partId": "part-gpu-current", "category": "GPU", "quantity": 1 }],
                                  "limit": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("GPU"))
                .andExpect(jsonPath("$.items[0].part.name").value("RTX 5070 Ti"))
                .andExpect(jsonPath("$.items[0].statusLabel").value("여유 있음"))
                .andExpect(jsonPath("$.rejectedCount").value(1));

        verify(currentUserService).requireUser(USER_TOKEN);
        verify(compatibleCandidateService).compatibleCandidates(eq(user), anyMap());
    }
}
