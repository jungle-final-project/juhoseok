package com.buildgraph.prototype.price;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PriceQueryService {
    private final JdbcTemplate jdbcTemplate;

    public PriceQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> alerts() {
        List<Map<String, Object>> items = alertRows();
        return MockData.map("items", items, "page", 0, "size", 20, "total", items.size());
    }

    public Map<String, Object> createAlert(Map<String, Object> request) {
        String partId = request == null ? defaultGpuPartId() : String.valueOf(request.getOrDefault("partId", defaultGpuPartId()));
        Integer targetPrice = numberValue(request == null ? null : request.get("targetPrice"), 850_000);
        List<Map<String, Object>> existing = alertRows(partId, targetPrice);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO price_alerts (user_id, part_id, target_price, status)
                VALUES (
                  (SELECT id FROM users WHERE email = 'user@example.com'),
                  (SELECT id FROM parts WHERE public_id = ?::uuid),
                  ?,
                  'ACTIVE'
                )
                """, partId, targetPrice);
        return alertRows(partId, targetPrice).get(0);
    }

    public Map<String, Object> priceJobs() {
        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                        SELECT pj.public_id::text AS id,
                               pj.status,
                               u.public_id::text AS requested_by,
                               pj.started_at,
                               pj.finished_at,
                               pj.error_summary,
                               pj.created_at
                        FROM price_jobs pj
                        JOIN users u ON u.id = pj.requested_by
                        WHERE pj.deleted_at IS NULL
                        ORDER BY pj.created_at DESC, pj.id DESC
                        """)
                .stream()
                .map(this::priceJobMap)
                .toList();
        return MockData.map("items", items, "page", 0, "size", 20, "total", items.size());
    }

    public Map<String, Object> runPriceJob() {
        List<Map<String, Object>> active = jdbcTemplate.queryForList("""
                SELECT pj.public_id::text AS id,
                       pj.status,
                       u.public_id::text AS requested_by,
                       pj.started_at,
                       pj.finished_at,
                       pj.error_summary,
                       pj.created_at
                FROM price_jobs pj
                JOIN users u ON u.id = pj.requested_by
                WHERE pj.status IN ('QUEUED', 'RUNNING')
                  AND pj.deleted_at IS NULL
                ORDER BY pj.created_at DESC
                LIMIT 1
                """);
        if (!active.isEmpty()) {
            return priceJobMap(active.get(0));
        }
        return priceJobMap(jdbcTemplate.queryForMap("""
                INSERT INTO price_jobs (requested_by, status)
                VALUES ((SELECT id FROM users WHERE email = 'admin@example.com'), 'QUEUED')
                RETURNING public_id::text AS id,
                          status,
                          (SELECT public_id::text FROM users WHERE email = 'admin@example.com') AS requested_by,
                          started_at,
                          finished_at,
                          error_summary,
                          created_at
                """));
    }

    private List<Map<String, Object>> alertRows() {
        return jdbcTemplate.queryForList("""
                        SELECT pa.part_id,
                               p.public_id::text AS part_public_id,
                               p.name AS part_name,
                               pa.target_price,
                               p.price AS current_price,
                               pa.status,
                               pa.created_at
                        FROM price_alerts pa
                        JOIN parts p ON p.id = pa.part_id
                        WHERE pa.deleted_at IS NULL
                        ORDER BY pa.created_at DESC, pa.id DESC
                        """)
                .stream()
                .map(this::alertMap)
                .toList();
    }

    private List<Map<String, Object>> alertRows(String partId, Integer targetPrice) {
        return jdbcTemplate.queryForList("""
                        SELECT pa.part_id,
                               p.public_id::text AS part_public_id,
                               p.name AS part_name,
                               pa.target_price,
                               p.price AS current_price,
                               pa.status,
                               pa.created_at
                        FROM price_alerts pa
                        JOIN parts p ON p.id = pa.part_id
                        WHERE pa.deleted_at IS NULL
                          AND pa.status = 'ACTIVE'
                          AND p.public_id = ?::uuid
                          AND pa.target_price = ?
                        ORDER BY pa.created_at DESC, pa.id DESC
                        """, partId, targetPrice)
                .stream()
                .map(this::alertMap)
                .toList();
    }

    private Map<String, Object> alertMap(Map<String, Object> row) {
        return MockData.map(
                "partId", DbValueMapper.string(row, "part_public_id"),
                "partName", DbValueMapper.string(row, "part_name"),
                "targetPrice", DbValueMapper.integer(row, "target_price"),
                "currentPrice", DbValueMapper.integer(row, "current_price"),
                "status", DbValueMapper.string(row, "status"),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }

    private Map<String, Object> priceJobMap(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "status", DbValueMapper.string(row, "status"),
                "requestedBy", DbValueMapper.string(row, "requested_by"),
                "startedAt", DbValueMapper.timestamp(row, "started_at"),
                "finishedAt", DbValueMapper.timestamp(row, "finished_at"),
                "errorSummary", DbValueMapper.string(row, "error_summary"),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }

    private String defaultGpuPartId() {
        return jdbcTemplate.queryForObject("""
                SELECT public_id::text
                FROM parts
                WHERE category = 'GPU'
                  AND deleted_at IS NULL
                ORDER BY id
                LIMIT 1
                """, String.class);
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
}
