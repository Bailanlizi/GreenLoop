package com.campus.trade.service;

import com.campus.trade.config.HybridSearchProperties;
import com.campus.trade.entity.Product;
import com.campus.trade.service.ai.AiEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductSemanticSearchService {
    private final AiEmbeddingService aiEmbeddingService;
    private final ProductEmbeddingService productEmbeddingService;
    private final HybridSearchProperties hybridSearchProperties;

    public ProductSemanticSearchService(AiEmbeddingService aiEmbeddingService,
                                        ProductEmbeddingService productEmbeddingService,
                                        HybridSearchProperties hybridSearchProperties) {
        this.aiEmbeddingService = aiEmbeddingService;
        this.productEmbeddingService = productEmbeddingService;
        this.hybridSearchProperties = hybridSearchProperties;
    }

    public List<Product> rank(String query, List<Product> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        Optional<List<Double>> queryEmbedding = aiEmbeddingService.embedText(query);
        if (queryEmbedding.isEmpty()) {
            return candidates;
        }
        List<String> ids = new ArrayList<>();
        for (Product product : candidates) {
            if (product != null && product.getId() != null) {
                ids.add(product.getId());
            }
        }
        Map<String, List<Double>> embeddings = productEmbeddingService.getEmbeddings(ids);
        if (embeddings.isEmpty()) {
            return candidates;
        }
        List<ScoredProduct> scored = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            Product product = candidates.get(index);
            double score = -1;
            if (product != null && product.getId() != null) {
                List<Double> embedding = embeddings.get(product.getId());
                if (embedding != null) {
                    score = cosineSimilarity(queryEmbedding.get(), embedding);
                }
            }
            scored.add(new ScoredProduct(product, score, index));
        }
        scored.sort(Comparator
                .comparingDouble(ScoredProduct::getScore).reversed()
                .thenComparingInt(ScoredProduct::getIndex));
        List<Product> result = new ArrayList<>(scored.size());
        for (ScoredProduct item : scored) {
            result.add(item.getProduct());
        }
        return result;
    }

    public List<Product> rankHybrid(String query, List<Product> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        Optional<List<Double>> queryEmbedding = aiEmbeddingService.embedText(query);
        List<String> ids = new ArrayList<>();
        for (Product product : candidates) {
            if (product != null && product.getId() != null) {
                ids.add(product.getId());
            }
        }
        Map<String, List<Double>> embeddings = queryEmbedding.isEmpty()
                ? Map.of()
                : productEmbeddingService.getEmbeddings(ids);

        Set<String> queryTokens = tokenize(query);
        List<ScoredProduct> scored = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            Product product = candidates.get(index);
            double lexicalScore = lexicalSimilarity(queryTokens, product);
            double semanticScore = -1;
            if (product != null && product.getId() != null && queryEmbedding.isPresent()) {
                List<Double> embedding = embeddings.get(product.getId());
                if (embedding != null) {
                    semanticScore = cosineSimilarity(queryEmbedding.get(), embedding);
                }
            }
            double score = combineScores(semanticScore, lexicalScore);
            scored.add(new ScoredProduct(product, score, index));
        }
        scored.sort(Comparator
                .comparingDouble(ScoredProduct::getScore).reversed()
                .thenComparingInt(ScoredProduct::getIndex));
        List<Product> result = new ArrayList<>(scored.size());
        for (ScoredProduct item : scored) {
            result.add(item.getProduct());
        }
        return result;
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

    private double combineScores(double semanticScore, double lexicalScore) {
        double semanticWeight = hybridSearchProperties.getSemanticWeight();
        double lexicalWeight = hybridSearchProperties.getLexicalWeight();
        double total = semanticWeight + lexicalWeight;
        if (total <= 0) {
            semanticWeight = 0.7;
            lexicalWeight = 0.3;
            total = 1.0;
        }
        semanticWeight /= total;
        lexicalWeight /= total;
        double minSemantic = hybridSearchProperties.getMinSemanticScore();
        double normalizedSemantic = semanticScore <= 0 ? 0 : semanticScore;
        if (normalizedSemantic < minSemantic) {
            return lexicalScore;
        }
        return semanticWeight * normalizedSemantic + lexicalWeight * lexicalScore;
    }

    private double lexicalSimilarity(Set<String> queryTokens, Product product) {
        if (queryTokens == null || queryTokens.isEmpty() || product == null) {
            return 0;
        }
        String text = (product.getTitle() == null ? "" : product.getTitle()) + " "
                + (product.getDescription() == null ? "" : product.getDescription());
        Set<String> productTokens = tokenize(text);
        if (productTokens.isEmpty()) {
            return 0;
        }
        int intersection = 0;
        for (String token : queryTokens) {
            if (productTokens.contains(token)) {
                intersection++;
            }
        }
        int union = queryTokens.size() + productTokens.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCjk(ch)) {
                flushBuffer(tokens, buffer);
                tokens.add(String.valueOf(ch));
                continue;
            }
            if (Character.isLetterOrDigit(ch)) {
                buffer.append(Character.toLowerCase(ch));
                continue;
            }
            flushBuffer(tokens, buffer);
        }
        flushBuffer(tokens, buffer);
        return tokens;
    }

    private void flushBuffer(Set<String> tokens, StringBuilder buffer) {
        if (buffer.length() == 0) {
            return;
        }
        tokens.add(buffer.toString());
        buffer.setLength(0);
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private static class ScoredProduct {
        private final Product product;
        private final double score;
        private final int index;

        private ScoredProduct(Product product, double score, int index) {
            this.product = product;
            this.score = score;
            this.index = index;
        }

        public Product getProduct() { return product; }
        public double getScore() { return score; }
        public int getIndex() { return index; }
    }
}
