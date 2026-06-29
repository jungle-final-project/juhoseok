package com.buildgraph.prototype.common;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import(CorsConfig.class)
class CorsConfigDefaultTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void defaultCorsAllowsLocalhostWebOrigins() throws Exception {
        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(1);

        mockMvc.perform(options("/api/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}

@WebMvcTest(HealthController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=https://d111111abcdef8.cloudfront.net")
class CorsConfigConfiguredTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void configuredCorsAllowsProductionCloudFrontOrigin() throws Exception {
        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(1);

        mockMvc.perform(options("/api/health")
                        .header("Origin", "https://d111111abcdef8.cloudfront.net")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://d111111abcdef8.cloudfront.net"));
    }
}
