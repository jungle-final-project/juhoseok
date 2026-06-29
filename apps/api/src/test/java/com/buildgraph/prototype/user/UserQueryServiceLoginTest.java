package com.buildgraph.prototype.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class UserQueryServiceLoginTest {
    private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
    private final PasswordService passwordService = new PasswordService();
    private final UserQueryService userQueryService = new UserQueryService(jdbcTemplate, passwordService);

    @Test
    void loginReturnsAuthResponseWhenPasswordMatches() {
        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(List.of(userRow(passwordService.hash("passw0rd!"))));

        Map<String, Object> response = userQueryService.login("user@example.com", "passw0rd!");

        assertThat(response).containsEntry("accessToken", "demo-access-user");
        assertThat(response).containsEntry("refreshToken", "demo-refresh-user");
        assertThat(response.get("user")).isInstanceOf(Map.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(List.of(userRow(passwordService.hash("passw0rd!"))));

        assertThatThrownBy(() -> userQueryService.login("user@example.com", "wrong-password"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );
    }

    @Test
    void loginRejectsMissingUser() {
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> userQueryService.login("missing@example.com", "passw0rd!"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );
    }

    private Map<String, Object> userRow(String passwordHash) {
        return Map.of(
                "id", "00000000-0000-4000-8000-000000001004",
                "email", "user@example.com",
                "password_hash", passwordHash,
                "name", "Demo User",
                "role", "USER"
        );
    }
}
