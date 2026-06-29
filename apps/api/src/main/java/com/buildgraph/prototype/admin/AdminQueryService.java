package com.buildgraph.prototype.admin;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminQueryService {
    private final JdbcTemplate jdbcTemplate;

    public AdminQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> dashboard() {
        Integer agentRunning = jdbcTemplate.queryForObject("""
                SELECT count(*)::int
                FROM agent_sessions
                WHERE status IN ('QUEUED', 'RUNNING', 'RAG_SEARCHED', 'TOOLS_CALLED', 'SUMMARY_READY', 'FALLBACK_READY')
                """, Integer.class);
        Integer openTickets = jdbcTemplate.queryForObject("""
                SELECT count(*)::int
                FROM as_tickets
                WHERE status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
                  AND deleted_at IS NULL
                """, Integer.class);
        Integer priceJobsRunning = jdbcTemplate.queryForObject("""
                SELECT count(*)::int
                FROM price_jobs
                WHERE status IN ('QUEUED', 'RUNNING')
                  AND deleted_at IS NULL
                """, Integer.class);
        return MockData.map(
                "agentRunning", agentRunning == null ? 0 : agentRunning,
                "openTickets", openTickets == null ? 0 : openTickets,
                "priceJobsRunning", priceJobsRunning == null ? 0 : priceJobsRunning,
                "degraded", false,
                "generatedAt", MockData.now()
        );
    }

    public Map<String, Object> auditLogs() {
        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                        SELECT action, target_type, target_id, metadata, created_at
                        FROM admin_audit_logs
                        ORDER BY created_at DESC, id DESC
                        LIMIT 20
                        """)
                .stream()
                .map(row -> MockData.map(
                        "action", DbValueMapper.string(row, "action"),
                        "targetType", DbValueMapper.string(row, "target_type"),
                        "targetId", DbValueMapper.string(row, "target_id"),
                        "metadata", DbValueMapper.json(row, "metadata", Map.of()),
                        "createdAt", DbValueMapper.timestamp(row, "created_at")
                ))
                .toList();
        return MockData.map("items", items);
    }
}
