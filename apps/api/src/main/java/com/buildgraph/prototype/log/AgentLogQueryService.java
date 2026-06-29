package com.buildgraph.prototype.log;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentLogQueryService {
    private final JdbcTemplate jdbcTemplate;

    public AgentLogQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> upload(MultipartFile file, Integer rangeMinutes, Boolean consentAccepted) {
        if (!Boolean.TRUE.equals(consentAccepted)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그 업로드 동의가 필요합니다.");
        }
        String fileName = file == null ? "agent-log.jsonl" : file.getOriginalFilename();
        long fileSize = file == null ? 0L : file.getSize();
        Integer minutes = rangeMinutes == null ? 30 : rangeMinutes;
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO agent_log_uploads (
                  user_id,
                  range_minutes,
                  status,
                  file_name,
                  file_size,
                  storage_path,
                  summary,
                  consent_accepted_at,
                  delete_after
                )
                VALUES (
                  (SELECT id FROM users WHERE email = 'user@example.com'),
                  ?,
                  'UPLOADED',
                  ?,
                  ?,
                  'seed/agent-logs/' || ?,
                  'Uploaded through DB-backed prototype API.',
                  now(),
                  now() + interval '30 days'
                )
                RETURNING public_id::text AS id, status, file_name, file_size, range_minutes, summary, created_at, delete_after
                """, minutes, fileName, fileSize, fileName);
        return logMap(row);
    }

    public Map<String, Object> detail(String id) {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, status, file_name, file_size, range_minutes, summary, created_at, delete_after
                        FROM agent_log_uploads
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .map(this::logMap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "로그 업로드를 찾을 수 없습니다."));
    }

    private Map<String, Object> logMap(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "status", DbValueMapper.string(row, "status"),
                "fileName", DbValueMapper.string(row, "file_name"),
                "fileSize", DbValueMapper.integer(row, "file_size"),
                "rangeMinutes", DbValueMapper.integer(row, "range_minutes"),
                "summary", DbValueMapper.string(row, "summary"),
                "createdAt", DbValueMapper.timestamp(row, "created_at"),
                "deleteAfter", DbValueMapper.timestamp(row, "delete_after")
        );
    }
}
