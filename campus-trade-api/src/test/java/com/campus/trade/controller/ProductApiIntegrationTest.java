package com.campus.trade.controller;

import com.campus.trade.dto.PageResult;
import com.campus.trade.entity.Product;
import com.campus.trade.service.ProductRiskService;
import com.campus.trade.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "orders.payment-expiration-enabled=false", "security.bootstrap-admin.enabled=false",
        "spring.mail.host=localhost", "spring.flyway.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ProductService productService;
    @MockBean private ProductRiskService productRiskService;

    @Test
    void productListReturnsThePaginatedPublicContract() throws Exception {
        Product product = new Product();
        product.setId("101");
        product.setTitle("测试商品");
        PageResult<Product> page = new PageResult<>(List.of(product));
        page.setTotal(41);
        when(productService.searchProducts(null, null, null, null, "latest", "standard", null, null))
                .thenReturn(page);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(41))
                .andExpect(jsonPath("$.data.list[0].id").value("101"));
    }

    @Test
    void productListForwardsFilterSortAndPagingParameters() throws Exception {
        PageResult<Product> page = new PageResult<>(List.of());
        when(productService.searchProducts("book", 2, 10d, 99d, "price_asc", "semantic", 3, 20))
                .thenReturn(page);

        mockMvc.perform(get("/products").param("keyword", "book").param("categoryId", "2")
                        .param("minPrice", "10").param("maxPrice", "99").param("orderBy", "price_asc")
                        .param("searchMode", "semantic").param("page", "3").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        verify(productService).searchProducts("book", 2, 10d, 99d, "price_asc", "semantic", 3, 20);
    }

    @Test
    void productListRejectsAnInvalidPriceRange() throws Exception {
        mockMvc.perform(get("/products").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"));
    }

    @Test
    void productMutationRequiresAuthentication() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/products")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
