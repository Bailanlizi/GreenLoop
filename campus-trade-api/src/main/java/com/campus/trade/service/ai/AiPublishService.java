package com.campus.trade.service.ai;

import com.campus.trade.config.AiProperties;
import com.campus.trade.dto.AiPublishRequest;
import com.campus.trade.dto.AiPublishResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class AiPublishService {
    private final AiClient aiClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiPublishService(AiClient aiClient, AiProperties properties, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AiPublishResponse suggest(AiPublishRequest request) {
        AiPublishResponse fallback = fallback(request);
        if (!properties.isEnabled()) {
            return fallback;
        }
        String systemPrompt = "你是电商文案助手，只返回严格的JSON，不要解释。";
        String userPrompt = buildUserPrompt(request);
        Optional<String> response = aiClient.chat(systemPrompt, userPrompt);
        if (response.isEmpty()) {
            return fallback;
        }
        String json = extractJson(response.get());
        if (json == null) {
            return fallback;
        }
        try {
            AiPublishResponse parsed = objectMapper.readValue(json, AiPublishResponse.class);
            return mergeFallback(parsed, fallback);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String buildUserPrompt(AiPublishRequest request) {
        String title = safe(request.getTitle());
        String description = safe(request.getDescription());
        String category = request.getCategoryId() == null ? "" : String.valueOf(request.getCategoryId());
        String price = request.getPrice() == null ? "" : request.getPrice().toPlainString();
        String condition = request.getConditionLevel() == null ? "" : String.valueOf(request.getConditionLevel());
        String delivery = request.getDeliveryOptions() == null ? "" : String.join(",", request.getDeliveryOptions());
        return "请基于以下信息输出JSON，字段: title, description, highlights[], tags[]。\n"
                + "标题需简洁有吸引力，描述要结构化、清晰。\n"
                + "原始信息:\n"
                + "title=" + title + "\n"
                + "description=" + description + "\n"
                + "categoryId=" + category + "\n"
                + "price=" + price + "\n"
                + "conditionLevel=" + condition + "\n"
                + "deliveryOptions=" + delivery + "\n";
    }

    private AiPublishResponse fallback(AiPublishRequest request) {
        AiPublishResponse response = new AiPublishResponse();
        response.setTitle(safe(request.getTitle()));
        response.setDescription(safe(request.getDescription()));
        response.setHighlights(Collections.emptyList());
        response.setTags(Collections.emptyList());
        return response;
    }

    private AiPublishResponse mergeFallback(AiPublishResponse parsed, AiPublishResponse fallback) {
        AiPublishResponse response = new AiPublishResponse();
        response.setTitle(safe(parsed.getTitle()).isEmpty() ? fallback.getTitle() : parsed.getTitle());
        response.setDescription(safe(parsed.getDescription()).isEmpty() ? fallback.getDescription() : parsed.getDescription());
        response.setHighlights(parsed.getHighlights() == null ? fallback.getHighlights() : parsed.getHighlights());
        response.setTags(parsed.getTags() == null ? fallback.getTags() : parsed.getTags());
        return response;
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return content.substring(start, end + 1);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
