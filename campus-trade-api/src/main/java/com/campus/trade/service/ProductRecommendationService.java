package com.campus.trade.service;

import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductRecommendationService {
    private final ProductMapper productMapper;
    private final ProductEmbeddingService productEmbeddingService;

    public ProductRecommendationService(ProductMapper productMapper,
                                        ProductEmbeddingService productEmbeddingService) {
        this.productMapper = productMapper;
        this.productEmbeddingService = productEmbeddingService;
    }

    public List<Product> recommendByEmbedding(String productId, int limit) {
        List<ScoredProduct> scored = scoreByEmbedding(productId, 50);
        if (scored.isEmpty()) {
            return List.of();
        }
        List<Product> result = new ArrayList<>();
        for (ScoredProduct item : scored) {
            result.add(item.getProduct());
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public List<ScoredProduct> scoreByEmbedding(String productId, int candidateLimit) {
        if (productId == null || productId.isBlank()) {
            return List.of();
        }
        Product product = productMapper.findProductById(productId);
        if (product == null || product.getCategoryId() == null) {
            return List.of();
        }
        Optional<List<Double>> baseEmbedding = productEmbeddingService.getEmbedding(productId);
        if (baseEmbedding.isEmpty()) {
            return List.of();
        }
        int limit = candidateLimit <= 0 ? 50 : candidateLimit;
        List<Product> candidates = productMapper.findAvailableByCategory(product.getCategoryId(), productId, limit);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Product candidate : candidates) {
            if (candidate != null && candidate.getId() != null) {
                ids.add(candidate.getId());
            }
        }
        Map<String, List<Double>> embeddings = productEmbeddingService.getEmbeddings(ids);
        if (embeddings.isEmpty()) {
            return List.of();
        }
        List<ScoredProduct> scored = new ArrayList<>();
        for (Product candidate : candidates) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            List<Double> embedding = embeddings.get(candidate.getId());
            if (embedding == null) {
                continue;
            }
            double score = cosineSimilarity(baseEmbedding.get(), embedding);
            scored.add(new ScoredProduct(candidate, normalizeScore(score)));
        }
        scored.sort(Comparator.comparingDouble(ScoredProduct::getScore).reversed());
        return scored;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        int size = Math.min(a.size(), b.size());
        if (size == 0) {
            return -1;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < size; i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? -1 : dot / denom;
    }

    public static class ScoredProduct {
        private final Product product;
        private final double score;

        private ScoredProduct(Product product, double score) {
            this.product = product;
            this.score = score;
        }

        public Product getProduct() { return product; }
        public double getScore() { return score; }
    }

    private double normalizeScore(double score) {
        return score <= 0 ? 0 : score;
    }
}
