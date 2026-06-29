package com.buildgraph.prototype.rag;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RagQueryService {
    private final JdbcTemplate jdbcTemplate;

    public RagQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> search(String query) {
        String normalizedQuery = blankToNull(query);
        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                        SELECT re.public_id::text AS id,
                               s.public_id::text AS agent_session_id,
                               re.source_id,
                               re.chunk_text,
                               re.summary,
                               re.score,
                               re.metadata
                        FROM rag_evidence re
                        LEFT JOIN agent_sessions s ON s.id = re.agent_session_id
                        WHERE (
                          ? IS NULL
                          OR lower(re.source_id) LIKE lower(concat('%', ?, '%'))
                          OR lower(re.summary) LIKE lower(concat('%', ?, '%'))
                          OR lower(re.chunk_text) LIKE lower(concat('%', ?, '%'))
                        )
                        ORDER BY re.score DESC NULLS LAST, re.id
                        """, normalizedQuery, normalizedQuery, normalizedQuery, normalizedQuery)
                .stream()
                .map(this::publicEvidenceMap)
                .toList();
        return MockData.map("items", items, "page", 0, "size", 20, "total", items.size());
    }

    public Map<String, Object> evidence(String id) {
        return evidenceRow(id).map(this::publicEvidenceMap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RAG 근거를 찾을 수 없습니다."));
    }

    public Map<String, Object> adminEvidence(String id) {
        return evidenceRow(id).map(this::adminEvidenceMap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RAG 근거를 찾을 수 없습니다."));
    }

    public List<Map<String, Object>> evidenceBySession(String sessionId) {
        return jdbcTemplate.queryForList(baseEvidenceSql() + """
                        WHERE s.public_id = ?::uuid
                        ORDER BY re.id
                        """, sessionId)
                .stream()
                .map(this::publicEvidenceMap)
                .toList();
    }

    private java.util.Optional<Map<String, Object>> evidenceRow(String id) {
        return jdbcTemplate.queryForList(baseEvidenceSql() + """
                        WHERE re.public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst();
    }

    private String baseEvidenceSql() {
        return """
                SELECT re.public_id::text AS id,
                       s.public_id::text AS agent_session_id,
                       re.source_id,
                       re.chunk_text,
                       re.summary,
                       re.score,
                       re.metadata
                FROM rag_evidence re
                LEFT JOIN agent_sessions s ON s.id = re.agent_session_id
                """;
    }

    private Map<String, Object> publicEvidenceMap(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "sourceId", DbValueMapper.string(row, "source_id"),
                "summary", DbValueMapper.string(row, "summary"),
                "score", row.get("score"),
                "metadata", DbValueMapper.json(row, "metadata", Map.of())
        );
    }

    private Map<String, Object> adminEvidenceMap(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "agentSessionId", DbValueMapper.string(row, "agent_session_id"),
                "sourceId", DbValueMapper.string(row, "source_id"),
                "chunkText", DbValueMapper.string(row, "chunk_text"),
                "summary", DbValueMapper.string(row, "summary"),
                "score", row.get("score"),
                "metadata", DbValueMapper.json(row, "metadata", Map.of())
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
