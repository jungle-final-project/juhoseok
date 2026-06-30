package com.buildgraph.prototype.rag;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.util.ArrayList;
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
        return search(query, null, null);
    }

    public Map<String, Object> search(String query, Integer page, Integer size) {
        String normalizedQuery = blankToNull(query);
        int safePage = validatePage(page);
        int safeSize = validateSize(size);
        int offset = safePage * safeSize;
        List<Object> params = new ArrayList<>();
        params.add(normalizedQuery);
        params.add(normalizedQuery);
        params.add(normalizedQuery);
        params.add(normalizedQuery);
        params.add(safeSize);
        params.add(offset);
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
                        ORDER BY CASE WHEN re.agent_session_id IS NULL THEN 0 ELSE 1 END,
                                 re.score DESC NULLS LAST,
                                 re.id
                        LIMIT ?
                        OFFSET ?
                        """, params.toArray())
                .stream()
                .map(this::publicEvidenceMap)
                .toList();
        Integer total = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM rag_evidence re
                WHERE (
                  ? IS NULL
                  OR lower(re.source_id) LIKE lower(concat('%', ?, '%'))
                  OR lower(re.summary) LIKE lower(concat('%', ?, '%'))
                  OR lower(re.chunk_text) LIKE lower(concat('%', ?, '%'))
                )
                """, Integer.class, normalizedQuery, normalizedQuery, normalizedQuery, normalizedQuery);
        return MockData.map("items", items, "page", safePage, "size", safeSize, "total", total == null ? 0 : total);
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

    private static int validatePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0 이상이어야 합니다.");
        }
        return page;
    }

    private static int validateSize(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1 이상 100 이하이어야 합니다.");
        }
        return size;
    }
}
