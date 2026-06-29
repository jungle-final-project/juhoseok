package com.buildgraph.prototype.agent;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpenAiResponsesClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiResponsesClient(
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .build();
        this.apiKey = blankToNull(apiKey);
        this.model = blankToNull(model) == null ? "gpt-4.1-mini" : model.trim();
    }

    public boolean isConfigured() {
        return apiKey != null;
    }

    public String model() {
        return model;
    }

    public String createSummary(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "OPENAI_API_KEY가 필요합니다.");
        }
        Map<String, Object> request = Map.of(
                "model", model,
                "instructions", systemPrompt,
                "input", userPrompt
        );
        Map<String, Object> response = restClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(MAP_RESPONSE);
        String output = extractOutputText(response);
        if (output == null || output.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI 응답에서 summary text를 찾을 수 없습니다.");
        }
        return output.trim();
    }

    @SuppressWarnings("unchecked")
    private static String extractOutputText(Map<String, Object> response) {
        Object directOutput = response == null ? null : response.get("output_text");
        if (directOutput instanceof String text && !text.isBlank()) {
            return text;
        }
        Object output = response == null ? null : response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> item)) {
                continue;
            }
            Object content = item.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }
                Object type = contentMap.get("type");
                Object text = contentMap.get("text");
                if ("output_text".equals(type) && text instanceof String textValue) {
                    builder.append(textValue);
                }
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null || value.isBlank() ? "https://api.openai.com/v1" : value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
