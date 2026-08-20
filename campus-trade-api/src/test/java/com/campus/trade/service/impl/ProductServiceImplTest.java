package com.campus.trade.service.impl;

import com.campus.trade.config.HybridRecommendationProperties;
import com.campus.trade.config.HybridSearchProperties;
import com.campus.trade.dto.PageResult;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.ProductDemandService;
import com.campus.trade.service.ProductEmbeddingService;
import com.campus.trade.service.ProductRecommendationService;
import com.campus.trade.service.ProductRiskService;
import com.campus.trade.service.ProductSemanticSearchService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {
    @Test
    void semanticSearchPaginatesTheRankedCandidateList() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductSemanticSearchService semanticSearch = mock(ProductSemanticSearchService.class);
        HybridSearchProperties searchProperties = new HybridSearchProperties();
        searchProperties.setCandidateLimit(100);
        List<Product> candidates = products(25);
        when(productMapper.findProductsWithLimit(eq("book"), eq(null), eq(null), eq(null), eq("latest"), eq(100)))
                .thenReturn(candidates);
        when(semanticSearch.rank(eq("book"), anyList())).thenReturn(candidates);

        ProductServiceImpl service = new ProductServiceImpl(
                productMapper, mock(ProductImageMapper.class), mock(ProductEmbeddingService.class), semanticSearch,
                mock(ProductRecommendationService.class), mock(ProductRiskService.class), mock(ProductDemandService.class),
                searchProperties, new HybridRecommendationProperties());

        PageResult<Product> result = service.searchProducts(" book ", null, null, null,
                "latest", "semantic", 2, 20);

        assertEquals(25, result.getTotal());
        assertEquals(5, result.getList().size());
        assertEquals("21", result.getList().get(0).getId());
    }

    private List<Product> products(int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Product product = new Product();
            product.setId(String.valueOf(i));
            products.add(product);
        }
        return products;
    }
}
