package com.buildgraph.prototype.user;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserQueryService {
    private final JdbcTemplate jdbcTemplate;

    public UserQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(String email) {
        Map<String, Object> user = findByEmail(email);
        String role = DbValueMapper.string(user, "role");
        return MockData.map(
                "accessToken", "demo-access-" + role.toLowerCase(),
                "refreshToken", "demo-refresh-" + role.toLowerCase(),
                "user", userMap(user)
        );
    }

    public Map<String, Object> signup(String name, String email, Boolean marketingAccepted) {
        List<Map<String, Object>> existing = findRowsByEmail(email);
        if (!existing.isEmpty()) {
            return userMap(existing.get(0));
        }
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO users (email, password_hash, name, role, terms_accepted_at, marketing_accepted_at)
                VALUES (?, 'seed-signup-password-hash', ?, 'USER', now(), CASE WHEN ? THEN now() ELSE NULL END)
                RETURNING public_id::text AS id, email, name, role, created_at
                """, email, name, Boolean.TRUE.equals(marketingAccepted));
        return userMap(row);
    }

    public Map<String, Object> me(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer demo-access-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String email = authorization.contains("admin") ? "admin@example.com" : "user@example.com";
        return userMap(findByEmail(email));
    }

    private Map<String, Object> findByEmail(String email) {
        return findRowsByEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "등록된 사용자를 찾을 수 없습니다."));
    }

    private List<Map<String, Object>> findRowsByEmail(String email) {
        return jdbcTemplate.queryForList("""
                SELECT public_id::text AS id, email, name, role, created_at
                FROM users
                WHERE email = ?
                  AND deleted_at IS NULL
                """, email);
    }

    private Map<String, Object> userMap(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "email", DbValueMapper.string(row, "email"),
                "name", DbValueMapper.string(row, "name"),
                "role", DbValueMapper.string(row, "role"),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }
}
