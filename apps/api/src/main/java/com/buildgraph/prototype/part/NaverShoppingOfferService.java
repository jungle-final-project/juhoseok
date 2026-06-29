package com.buildgraph.prototype.part;

import com.buildgraph.prototype.common.MockData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NaverShoppingOfferService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> CATEGORIES = Set.of("CPU", "GPU", "RAM", "MOTHERBOARD", "STORAGE", "PSU", "CASE", "COOLER");
    private static final String SOURCE = "NAVER_SHOPPING_SEARCH";

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;
    private final JdbcTemplate jdbcTemplate;

    public NaverShoppingOfferService(
            @Value("${naver.search.client-id:}") String clientId,
            @Value("${naver.search.client-secret:}") String clientSecret,
            @Value("${naver.search.base-url:https://openapi.naver.com}") String baseUrl,
            JdbcTemplate jdbcTemplate
    ) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> refreshOffers(String category, Integer limit, Boolean force) {
        String normalizedCategory = normalizeCategory(category);
        int safeLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        boolean forceRefresh = Boolean.TRUE.equals(force);

        if (!configured()) {
            return MockData.map(
                    "configured", false,
                    "category", normalizedCategory,
                    "limit", safeLimit,
                    "attempted", 0,
                    "updated", 0,
                    "skipped", 0,
                    "failed", 0,
                    "message", "NAVER_SEARCH_CLIENT_ID 또는 NAVER_SEARCH_CLIENT_SECRET이 설정되지 않았습니다."
            );
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.public_id::text AS public_id, p.category, p.name, p.manufacturer
                FROM parts p
                LEFT JOIN part_external_offers peo
                  ON peo.part_id = p.id
                 AND peo.source = 'NAVER_SHOPPING_SEARCH'
                 AND peo.deleted_at IS NULL
                WHERE p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                """);
        if (normalizedCategory != null) {
            sql.append(" AND p.category = ?");
            params.add(normalizedCategory);
        }
        if (!forceRefresh) {
            sql.append(" AND (peo.id IS NULL OR peo.refreshed_at < now() - interval '7 days')");
        }
        sql.append(" ORDER BY p.category, p.id LIMIT ?");
        params.add(safeLimit);

        List<Map<String, Object>> parts = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        int attempted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (Map<String, Object> part : parts) {
            attempted += 1;
            String name = stringValue(part.get("name"));
            String manufacturer = stringValue(part.get("manufacturer"));
            String query = searchQuery(name, manufacturer);
            Optional<Map<String, Object>> offer = fetchOffer(query);
            if (offer.isEmpty()) {
                skipped += 1;
                continue;
            }
            try {
                upsertOffer(((Number) part.get("id")).longValue(), query, offer.get());
                updated += 1;
            } catch (RuntimeException ignored) {
                failed += 1;
            }
        }

        return MockData.map(
                "configured", true,
                "category", normalizedCategory,
                "limit", safeLimit,
                "force", forceRefresh,
                "attempted", attempted,
                "updated", updated,
                "skipped", skipped,
                "failed", failed
        );
    }

    private Optional<Map<String, Object>> fetchOffer(String query) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/shop.json")
                            .queryParam("query", query)
                            .queryParam("display", 1)
                            .queryParam("start", 1)
                            .queryParam("sort", "sim")
                            .queryParam("exclude", "used:rental:cbshop")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("items") instanceof List<?> items) || items.isEmpty()) {
                return Optional.empty();
            }
            if (!(items.get(0) instanceof Map<?, ?> item)) {
                return Optional.empty();
            }

            return Optional.of(MockData.map(
                    "title", cleanText(stringValue(item.get("title"))),
                    "imageUrl", stringValue(item.get("image")),
                    "supplierName", stringValue(item.get("mallName")),
                    "offerUrl", stringValue(item.get("link")),
                    "lowPrice", integerValue(item.get("lprice")),
                    "source", SOURCE,
                    "rawPayload", item
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private void upsertOffer(long partId, String query, Map<String, Object> offer) {
        jdbcTemplate.update("""
                INSERT INTO part_external_offers (
                  part_id,
                  source,
                  search_query,
                  title,
                  image_url,
                  supplier_name,
                  offer_url,
                  low_price,
                  raw_payload,
                  refreshed_at,
                  created_at,
                  updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now(), now(), now())
                ON CONFLICT (part_id, source) WHERE deleted_at IS NULL
                DO UPDATE SET
                  search_query = EXCLUDED.search_query,
                  title = EXCLUDED.title,
                  image_url = EXCLUDED.image_url,
                  supplier_name = EXCLUDED.supplier_name,
                  offer_url = EXCLUDED.offer_url,
                  low_price = EXCLUDED.low_price,
                  raw_payload = EXCLUDED.raw_payload,
                  refreshed_at = now(),
                  updated_at = now()
                """,
                partId,
                SOURCE,
                query,
                offer.get("title"),
                offer.get("imageUrl"),
                offer.get("supplierName"),
                offer.get("offerUrl"),
                offer.get("lowPrice"),
                json(offer.get("rawPayload"))
        );
    }

    private boolean configured() {
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }

    private static String searchQuery(String name, String manufacturer) {
        if (!StringUtils.hasText(manufacturer) || manufacturer.endsWith("Partner")) {
            return name;
        }
        return manufacturer + " " + name;
    }

    private static String normalizeCategory(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String upper = value.trim().toUpperCase();
        if (!CATEGORIES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 부품 category입니다.");
        }
        return upper;
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private static String cleanText(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
