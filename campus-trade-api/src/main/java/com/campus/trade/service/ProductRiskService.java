package com.campus.trade.service;

import com.campus.trade.dto.ProductDTO;
import com.campus.trade.dto.RiskCheckResponse;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductRisk;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.ProductRiskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductRiskService {
    private final ProductMapper productMapper;
    private final ProductRiskMapper productRiskMapper;
    private final ObjectMapper objectMapper;

    public ProductRiskService(ProductMapper productMapper,
                              ProductRiskMapper productRiskMapper,
                              ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.productRiskMapper = productRiskMapper;
        this.objectMapper = objectMapper;
    }

    public RiskCheckResponse evaluate(ProductDTO dto, String sellerId) {
        if (dto == null) {
            return new RiskCheckResponse("LOW", List.of());
        }
        return evaluateInternal(sellerId, dto.getTitle(), dto.getDescription(), dto.getPrice(),
                dto.getCategoryId(), dto.getCoverImage(), null);
    }

    public RiskCheckResponse evaluate(Product product) {
        if (product == null) {
            return new RiskCheckResponse("LOW", List.of());
        }
        return evaluateInternal(product.getSellerId(), product.getTitle(), product.getDescription(),
                product.getPrice(), product.getCategoryId(), product.getCoverImage(), product.getId());
    }

    public void recordIfRisk(String productId, RiskCheckResponse response) {
        if (response == null || productId == null || "LOW".equalsIgnoreCase(response.getRiskLevel())) {
            return;
        }
        try {
            ProductRisk risk = new ProductRisk();
            risk.setProductId(productId);
            risk.setRiskLevel(response.getRiskLevel());
            risk.setReasons(objectMapper.writeValueAsString(response.getReasons()));
            productRiskMapper.insert(risk);
        } catch (Exception ignored) {
        }
    }

    private RiskCheckResponse evaluateInternal(String sellerId, String title, String description,
                                               BigDecimal price, Integer categoryId, String coverImage,
                                               String excludeId) {
        List<String> reasons = new ArrayList<>();
        boolean duplicateImage = false;
        boolean highSimilarity = false;
        boolean priceOutlier = false;

        if (title == null || title.trim().length() < 4) {
            reasons.add("标题过短，可能影响检索与曝光");
        }
        if (description == null || description.trim().length() < 20) {
            reasons.add("描述过短，可能影响成交转化");
        }

        if (sellerId != null && !sellerId.isBlank()) {
            List<Product> recent = productMapper.findRecentProductsBySeller(sellerId, 30, 20, excludeId);
            for (Product item : recent) {
                if (item == null) {
                    continue;
                }
                if (coverImage != null && coverImage.equals(item.getCoverImage())) {
                    duplicateImage = true;
                }
                double sim = jaccardSimilarity(title, item.getTitle());
                if (sim >= 0.85) {
                    highSimilarity = true;
                }
            }
        }

        if (duplicateImage) {
            reasons.add("封面图片与历史商品重复，疑似重复发布");
        }
        if (highSimilarity) {
            reasons.add("标题相似度较高，疑似重复发布");
        }

        if (categoryId != null && price != null) {
            Double avgPrice = productMapper.getCategoryAveragePrice(categoryId);
            if (avgPrice != null && avgPrice > 0) {
                BigDecimal avg = BigDecimal.valueOf(avgPrice);
                if (price.compareTo(avg.multiply(BigDecimal.valueOf(3))) > 0
                        || price.compareTo(avg.multiply(BigDecimal.valueOf(0.2))) < 0) {
                    priceOutlier = true;
                }
            }
        }

        if (priceOutlier) {
            reasons.add("价格偏离同类均值，可能影响成交或触发异常");
        }

        String level = resolveLevel(reasons, duplicateImage, highSimilarity, priceOutlier);
        return new RiskCheckResponse(level, reasons);
    }

    private String resolveLevel(List<String> reasons, boolean duplicateImage, boolean highSimilarity, boolean priceOutlier) {
        if (duplicateImage || highSimilarity) {
            return "HIGH";
        }
        if (priceOutlier || reasons.size() >= 2) {
            return "MEDIUM";
        }
        return reasons.isEmpty() ? "LOW" : "LOW";
    }

    private double jaccardSimilarity(String a, String b) {
        Set<String> setA = tokenize(a);
        Set<String> setB = tokenize(b);
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null) {
            return Set.of();
        }
        String[] parts = text.toLowerCase().split("[^\\p{L}\\p{N}]+");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
