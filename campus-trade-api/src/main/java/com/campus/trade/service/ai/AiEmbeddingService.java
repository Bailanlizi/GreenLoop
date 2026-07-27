package com.campus.trade.service.ai;

import com.campus.trade.config.AiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AiEmbeddingService {
    private final AiClient aiClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiEmbeddingService(AiClient aiClient, AiProperties properties, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<List<Double>> embedText(String text) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        return aiClient.embed(text);
    }

    public Optional<String> embedTextAsJson(String text) {
        Optional<List<Double>> embedding = embedText(text);
        if (embedding.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.writeValueAsString(embedding.get()));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
