package com.campus.trade.service.ai;

import com.campus.trade.config.AiProperties;
import com.campus.trade.dto.AiPriceSuggestionRequest;
import com.campus.trade.dto.AiPriceSuggestionResponse;
import com.campus.trade.dto.CategoryPriceStats;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AiPriceSuggestionService {
    private final AiClient aiClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;

    public AiPriceSuggestionService(AiClient aiClient,
                                    AiProperties properties,
                                    ObjectMapper objectMapper,
                                    ProductMapper productMapper) {
        this.aiClient = aiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.productMapper = productMapper;
    }

    public AiPriceSuggestionResponse suggest(AiPriceSuggestionRequest request) {
        CategoryPriceStats stats = request.getCategoryId() == null ? null : productMapper.getCategoryPriceStats(request.getCategoryId());
        AiPriceSuggestionResponse fallback = buildFallback(request, stats);
        if (!properties.isEnabled()) {
            return fallback;
        }
        String systemPrompt = "你是二手商品定价助手，只返回严格JSON，不要解释。";
        String userPrompt = buildUserPrompt(request, stats);
        Optional<String> response = aiClient.chat(systemPrompt, userPrompt);
        if (response.isEmpty()) {
            return fallback;
        }
        String json = extractJson(response.get());
        if (json == null) {
            return fallback;
        }
        try {
            AiPriceSuggestionResponse parsed = objectMapper.readValue(json, AiPriceSuggestionResponse.class);
            return mergeFallback(parsed, fallback);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String buildUserPrompt(AiPriceSuggestionRequest request, CategoryPriceStats stats) {
        String title = safe(request.getTitle());
        String description = safe(request.getDescription());
        String categoryId = request.getCategoryId() == null ? "" : String.valueOf(request.getCategoryId());
        String condition = request.getConditionLevel() == null ? "" : String.valueOf(request.getConditionLevel());
        String delivery = request.getDeliveryOptions() == null ? "" : String.join(",", request.getDeliveryOptions());
        String currentPrice = request.getCurrentPrice() == null ? "" : request.getCurrentPrice().toPlainString();
        String statsText = stats == null ? "" : String.format("avg=%s,min=%s,max=%s,count=%s",
                toPlain(stats.getAvgPrice()), toPlain(stats.getMinPrice()), toPlain(stats.getMaxPrice()), String.valueOf(stats.getTotalCount()));

        List<Product> samples = request.getCategoryId() == null ? Collections.emptyList()
                : productMapper.findProductsWithLimit(title, request.getCategoryId(), null, null, "latest", 8);
        String sampleText = samples == null ? "" : samples.stream()
                .map(p -> String.format("%s|%s", safe(p.getTitle()), toPlain(p.getPrice())))
                .collect(Collectors.joining(", "));

        return "请基于以下信息输出JSON，字段：suggestedMin, suggestedMax, suggestedPrice, summary, tips[]。\n"
                + "标题=" + title + "\n"
                + "描述=" + description + "\n"
                + "categoryId=" + categoryId + "\n"
                + "conditionLevel=" + condition + "\n"
                + "deliveryOptions=" + delivery + "\n"
                + "currentPrice=" + currentPrice + "\n"
                + "市场统计=" + statsText + "\n"
                + "同类样本(标题|价格)=" + sampleText + "\n"
                + "summary简短说明市场区间与策略，tips给出3条提升竞争力建议。";
    }

    private AiPriceSuggestionResponse buildFallback(AiPriceSuggestionRequest request, CategoryPriceStats stats) {
        AiPriceSuggestionResponse response = new AiPriceSuggestionResponse();
        if (stats != null) {
            response.setCategoryAvg(scale(stats.getAvgPrice()));
            response.setCategoryMin(scale(stats.getMinPrice()));
            response.setCategoryMax(scale(stats.getMaxPrice()));
            response.setCategoryCount(stats.getTotalCount());
            BigDecimal base = stats.getAvgPrice() == null ? request.getCurrentPrice() : stats.getAvgPrice();
            if (base != null) {
                response.setSuggestedMin(scale(base.multiply(BigDecimal.valueOf(0.9))));
                response.setSuggestedMax(scale(base.multiply(BigDecimal.valueOf(1.1))));
                response.setSuggestedPrice(scale(base));
            }
            response.setSummary("基于同类商品平均价给出参考区间");
        } else {
            response.setSuggestedPrice(scale(request.getCurrentPrice()));
            response.setSummary("暂无同类统计，建议参考商品成色与成交速度");
        }
        response.setTips(Collections.emptyList());
        return response;
    }

    private AiPriceSuggestionResponse mergeFallback(AiPriceSuggestionResponse parsed, AiPriceSuggestionResponse fallback) {
        AiPriceSuggestionResponse response = new AiPriceSuggestionResponse();
        response.setSuggestedMin(optional(parsed.getSuggestedMin(), fallback.getSuggestedMin()));
        response.setSuggestedMax(optional(parsed.getSuggestedMax(), fallback.getSuggestedMax()));
        response.setSuggestedPrice(optional(parsed.getSuggestedPrice(), fallback.getSuggestedPrice()));
        response.setSummary(safe(parsed.getSummary()).isEmpty() ? fallback.getSummary() : parsed.getSummary());
        response.setTips(parsed.getTips() == null ? fallback.getTips() : parsed.getTips());
        response.setCategoryAvg(fallback.getCategoryAvg());
        response.setCategoryMin(fallback.getCategoryMin());
        response.setCategoryMax(fallback.getCategoryMax());
        response.setCategoryCount(fallback.getCategoryCount());
        return response;
    }

    private BigDecimal optional(BigDecimal primary, BigDecimal fallback) {
        return primary == null ? fallback : primary;
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

    private String toPlain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
