package com.buildgraph.prototype.build;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuildQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    public BuildQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> parse(Map<String, Object> request) {
        String message = String.valueOf(request.getOrDefault("message", "QHD 게임용 PC"));
        Integer budget = numberValue(request.get("budget"), 2_000_000);
        Map<String, Object> parsedContext = MockData.map(
                "usageTags", List.of("GAMING", "DEVELOPMENT"),
                "budget", budget,
                "preferredVendors", List.of("NVIDIA")
        );
        String id = jdbcTemplate.queryForObject("""
                INSERT INTO requirements (user_id, raw_message, budget, usage_tags, parsed_context)
                VALUES ((SELECT id FROM users WHERE email = 'user@example.com'), ?, ?, ARRAY['GAMING','DEVELOPMENT'], ?::jsonb)
                RETURNING public_id::text
                """, String.class, message, budget, json(parsedContext));
        return MockData.map(
                "id", id,
                "rawMessage", message,
                "budget", budget,
                "usageTags", List.of("GAMING", "DEVELOPMENT"),
                "parsedContext", parsedContext
        );
    }

    public Map<String, Object> recommendations() {
        return MockData.map(
                "agentSessionId", firstAgentSessionId(),
                "recommendations", builds(),
                "warnings", List.of(),
                "evidenceIds", evidenceIds()
        );
    }

    public List<Map<String, Object>> builds() {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, name, total_price, confidence, warnings, created_at
                        FROM builds
                        ORDER BY created_at, id
                        """)
                .stream()
                .map(this::buildSummary)
                .toList();
    }

    public Map<String, Object> buildDetail(String id) {
        Map<String, Object> row = jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, name, total_price, confidence, warnings, created_at
                        FROM builds
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build를 찾을 수 없습니다."));
        Map<String, Object> summary = buildSummary(row);
        return MockData.map(
                "id", summary.get("id"),
                "name", summary.get("name"),
                "totalPrice", summary.get("totalPrice"),
                "confidence", summary.get("confidence"),
                "items", summary.get("items"),
                "warnings", summary.get("warnings"),
                "evidenceIds", summary.get("evidenceIds"),
                "changeableCategories", summary.get("changeableCategories"),
                "createdAt", summary.get("createdAt"),
                "toolResults", toolResults(id)
        );
    }

    public Map<String, Object> changePart(String id, Map<String, Object> request) {
        String category = request == null ? "GPU" : String.valueOf(request.getOrDefault("category", "GPU"));
        String selectedPartId = request == null ? defaultPartId(category) : String.valueOf(request.getOrDefault("partId", defaultPartId(category)));
        Map<String, Object> previous = jdbcTemplate.queryForList("""
                        SELECT p.public_id::text AS part_id, b.total_price
                        FROM builds b
                        LEFT JOIN build_items bi ON bi.build_id = b.id AND bi.category = ?
                        LEFT JOIN parts p ON p.id = bi.part_id
                        WHERE b.public_id = ?::uuid
                        LIMIT 1
                        """, category, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build를 찾을 수 없습니다."));
        return MockData.map(
                "buildId", id,
                "category", category,
                "previousPartId", DbValueMapper.string(previous, "part_id"),
                "selectedPartId", selectedPartId,
                "totalPrice", DbValueMapper.integer(previous, "total_price"),
                "diff", MockData.map("price", 0, "source", "db-seed skeleton"),
                "warnings", List.of()
        );
    }

    private Map<String, Object> buildSummary(Map<String, Object> row) {
        String id = DbValueMapper.string(row, "id");
        return MockData.map(
                "id", id,
                "name", DbValueMapper.string(row, "name"),
                "totalPrice", DbValueMapper.integer(row, "total_price"),
                "confidence", DbValueMapper.string(row, "confidence"),
                "items", buildItems(id),
                "warnings", DbValueMapper.json(row, "warnings", List.of()),
                "evidenceIds", evidenceIds(id),
                "changeableCategories", List.of("GPU", "RAM"),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }

    private List<Map<String, Object>> buildItems(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT p.public_id::text AS id, p.category, p.name, p.manufacturer, bi.price, p.status, p.attributes
                        FROM build_items bi
                        JOIN builds b ON b.id = bi.build_id
                        JOIN parts p ON p.id = bi.part_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY bi.id
                        """, buildId)
                .stream()
                .map(row -> MockData.map(
                        "id", DbValueMapper.string(row, "id"),
                        "category", DbValueMapper.string(row, "category"),
                        "name", DbValueMapper.string(row, "name"),
                        "manufacturer", DbValueMapper.string(row, "manufacturer"),
                        "price", DbValueMapper.integer(row, "price"),
                        "status", DbValueMapper.string(row, "status"),
                        "attributes", DbValueMapper.json(row, "attributes", Map.of())
                ))
                .toList();
    }

    private List<String> evidenceIds() {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text
                        FROM rag_evidence
                        ORDER BY id
                        """, String.class)
                .stream()
                .limit(3)
                .toList();
    }

    private List<String> evidenceIds(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT re.public_id::text
                        FROM rag_evidence re
                        JOIN agent_sessions s ON s.id = re.agent_session_id
                        JOIN builds b ON b.id = s.build_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY re.id
                        """, String.class, buildId);
    }

    private List<Map<String, Object>> toolResults(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT ti.status, ti.confidence, ti.summary, ti.result_payload
                        FROM tool_invocations ti
                        JOIN agent_sessions s ON s.id = ti.agent_session_id
                        JOIN builds b ON b.id = s.build_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY ti.id
                        """, buildId)
                .stream()
                .map(row -> MockData.map(
                        "status", DbValueMapper.string(row, "status"),
                        "confidence", DbValueMapper.string(row, "confidence"),
                        "summary", DbValueMapper.string(row, "summary"),
                        "details", DbValueMapper.json(row, "result_payload", Map.of())
                ))
                .toList();
    }

    private String firstAgentSessionId() {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT public_id::text
                FROM agent_sessions
                ORDER BY id
                LIMIT 1
                """, String.class);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String defaultPartId(String category) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT public_id::text
                FROM parts
                WHERE category = ?
                  AND deleted_at IS NULL
                ORDER BY id
                LIMIT 1
                """, String.class, category);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Integer numberValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        return Integer.valueOf(value.toString());
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 변환에 실패했습니다.", e);
        }
    }
}
