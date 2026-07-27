package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.AiPriceSuggestionRequest;
import com.campus.trade.dto.AiPriceSuggestionResponse;
import com.campus.trade.dto.AiPublishRequest;
import com.campus.trade.dto.AiPublishResponse;
import com.campus.trade.service.ProductEmbeddingService;
import com.campus.trade.service.ai.AiPublishService;
import com.campus.trade.service.ai.AiPriceSuggestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiPublishService aiPublishService;
    private final ProductEmbeddingService productEmbeddingService;
    private final AiPriceSuggestionService aiPriceSuggestionService;

    public AiController(AiPublishService aiPublishService,
                        ProductEmbeddingService productEmbeddingService,
                        AiPriceSuggestionService aiPriceSuggestionService) {
        this.aiPublishService = aiPublishService;
        this.productEmbeddingService = productEmbeddingService;
        this.aiPriceSuggestionService = aiPriceSuggestionService;
    }

    @PostMapping("/publish/suggest")
    @PreAuthorize("isAuthenticated()")
    public Result<AiPublishResponse> suggestPublish(@RequestBody AiPublishRequest request) {
        return Result.success(aiPublishService.suggest(request));
    }

    @PostMapping("/price/suggest")
    @PreAuthorize("isAuthenticated()")
    public Result<AiPriceSuggestionResponse> suggestPrice(@RequestBody AiPriceSuggestionRequest request) {
        return Result.success(aiPriceSuggestionService.suggest(request));
    }

    @PostMapping("/embeddings/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Integer>> rebuildEmbeddings(@RequestParam(defaultValue = "200") int batchSize) {
        int updated = productEmbeddingService.rebuildEmbeddings(batchSize);
        return Result.success(Map.of("updated", updated));
    }
}
