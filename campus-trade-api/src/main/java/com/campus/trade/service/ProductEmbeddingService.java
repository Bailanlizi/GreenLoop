package com.campus.trade.service;

import com.campus.trade.config.AiProperties;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductEmbedding;
import com.campus.trade.mapper.ProductEmbeddingMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.ai.AiEmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductEmbeddingService {
    private final ProductEmbeddingMapper productEmbeddingMapper;
    private final ProductMapper productMapper;
    private final AiEmbeddingService aiEmbeddingService;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public ProductEmbeddingService(ProductEmbeddingMapper productEmbeddingMapper,
                                   ProductMapper productMapper,
                                   AiEmbeddingService aiEmbeddingService,
                                   AiProperties properties,
                                   ObjectMapper objectMapper) {
        this.productEmbeddingMapper = productEmbeddingMapper;
        this.productMapper = productMapper;
        this.aiEmbeddingService = aiEmbeddingService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean upsertEmbedding(Product product) {
        if (product == null || product.getId() == null) {
            return false;
        }
        String text = buildText(product);
        Optional<String> embeddingJson = aiEmbeddingService.embedTextAsJson(text);
        if (embeddingJson.isEmpty()) {
            return false;
        }
        productEmbeddingMapper.upsertEmbedding(product.getId(), embeddingJson.get(), properties.getEmbeddingModel());
        return true;
    }

    public Optional<List<Double>> getEmbedding(String productId) {
        ProductEmbedding embedding = productEmbeddingMapper.findByProductId(productId);
        if (embedding == null || embedding.getEmbedding() == null) {
            return Optional.empty();
        }
        return parseEmbedding(embedding.getEmbedding());
    }

    public Map<String, List<Double>> getEmbeddings(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductEmbedding> embeddings = productEmbeddingMapper.findByProductIds(productIds);
        if (embeddings == null || embeddings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<Double>> result = new HashMap<>();
        for (ProductEmbedding embedding : embeddings) {
            if (embedding.getProductId() == null || embedding.getEmbedding() == null) {
                continue;
            }
            Optional<List<Double>> parsed = parseEmbedding(embedding.getEmbedding());
            parsed.ifPresent(list -> result.put(embedding.getProductId(), list));
        }
        return result;
    }

    public int rebuildEmbeddings(int batchSize) {
        int size = batchSize <= 0 ? 100 : batchSize;
        int offset = 0;
        int updated = 0;
        while (true) {
            List<Product> batch = productMapper.findProductsForEmbedding(offset, size);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (Product product : batch) {
                if (upsertEmbedding(product)) {
                    updated++;
                }
            }
            if (batch.size() < size) {
                break;
            }
            offset += size;
        }
        return updated;
    }

    private Optional<List<Double>> parseEmbedding(String embeddingJson) {
        try {
            List<Double> embedding = objectMapper.readValue(
                    embeddingJson,
                    new TypeReference<List<Double>>() {}
            );
            return embedding == null || embedding.isEmpty() ? Optional.empty() : Optional.of(embedding);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String buildText(Product product) {
        List<String> parts = new ArrayList<>();
        if (product.getTitle() != null && !product.getTitle().isBlank()) {
            parts.add(product.getTitle().trim());
        }
        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            parts.add(product.getDescription().trim());
        }
        return String.join(" ", parts);
    }
}
