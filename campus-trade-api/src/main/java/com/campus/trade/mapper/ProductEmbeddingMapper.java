package com.campus.trade.mapper;

import com.campus.trade.entity.ProductEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductEmbeddingMapper {
    ProductEmbedding findByProductId(@Param("productId") String productId);
    List<ProductEmbedding> findByProductIds(@Param("productIds") List<String> productIds);
    void upsertEmbedding(@Param("productId") String productId,
                         @Param("embedding") String embedding,
                         @Param("model") String model);
}
