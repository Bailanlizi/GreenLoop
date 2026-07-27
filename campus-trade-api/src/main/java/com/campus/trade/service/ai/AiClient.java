package com.campus.trade.service.ai;

import com.campus.trade.config.AiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiClient {
    private final RestTemplate restTemplate;
    private final AiProperties properties;

    public AiClient(RestTemplate aiRestTemplate, AiProperties properties) {
        this.restTemplate = aiRestTemplate;
        this.properties = properties;
    }

    public Optional<List<Double>> embed(String input) {
        if (!isReady() || input == null || input.isBlank()) {
            return Optional.empty();
        }
        String url = properties.getBaseUrl() + "/embeddings";
        Map<String, Object> body = Map.of(
                "model", properties.getEmbeddingModel(),
                "input", input
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return Optional.empty();
            }
            Object dataObj = responseBody.get("data");
            if (!(dataObj instanceof List)) {
                return Optional.empty();
            }
            List<?> dataList = (List<?>) dataObj;
            if (dataList.isEmpty()) {
                return Optional.empty();
            }
            Object first = dataList.get(0);
            if (!(first instanceof Map)) {
                return Optional.empty();
            }
            Object embeddingObj = ((Map<?, ?>) first).get("embedding");
            if (!(embeddingObj instanceof List)) {
                return Optional.empty();
            }
            List<?> rawEmbedding = (List<?>) embeddingObj;
            List<Double> embedding = new ArrayList<>(rawEmbedding.size());
            for (Object value : rawEmbedding) {
                if (value instanceof Number) {
                    embedding.add(((Number) value).doubleValue());
                }
            }
            return embedding.isEmpty() ? Optional.empty() : Optional.of(embedding);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<String> chat(String systemPrompt, String userPrompt) {
        if (!isReady()) {
            return Optional.empty();
        }
        String url = properties.getBaseUrl() + "/chat/completions";
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
        Map<String, Object> body = Map.of(
                "model", properties.getChatModel(),
                "temperature", properties.getTemperature(),
                "messages", messages
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return Optional.empty();
            }
            Object choicesObj = responseBody.get("choices");
            if (!(choicesObj instanceof List)) {
                return Optional.empty();
            }
            List<?> choices = (List<?>) choicesObj;
            if (choices.isEmpty()) {
                return Optional.empty();
            }
            Object first = choices.get(0);
            if (!(first instanceof Map)) {
                return Optional.empty();
            }
            Object messageObj = ((Map<?, ?>) first).get("message");
            if (!(messageObj instanceof Map)) {
                return Optional.empty();
            }
            Object contentObj = ((Map<?, ?>) messageObj).get("content");
            if (!(contentObj instanceof String)) {
                return Optional.empty();
            }
            String content = ((String) contentObj).trim();
            return content.isEmpty() ? Optional.empty() : Optional.of(content);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private boolean isReady() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        return headers;
    }
}
