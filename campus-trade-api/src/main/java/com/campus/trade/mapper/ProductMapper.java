package com.campus.trade.mapper;

import com.campus.trade.dto.DailyStatsDTO;
import com.campus.trade.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {
    List<Product> findProducts(@Param("keyword") String keyword,
                               @Param("categoryId") Integer categoryId,
                               @Param("minPrice") Double minPrice,
                               @Param("maxPrice") Double maxPrice,
                               @Param("orderBy") String orderBy);

    List<Product> findProductsWithLimit(@Param("keyword") String keyword,
                                        @Param("categoryId") Integer categoryId,
                                        @Param("minPrice") Double minPrice,
                                        @Param("maxPrice") Double maxPrice,
                                        @Param("orderBy") String orderBy,
                                        @Param("limit") int limit);

    Product findProductById(String productId);
    void insertProduct(Product product);
    void updateProduct(Product product);
    void updateProductStatus(@Param("productId") String productId, @Param("status") String status);
    int lockProductIfAvailable(@Param("productId") String productId);
    int updateProductStatusIfCurrent(@Param("productId") String productId,
                                     @Param("currentStatus") String currentStatus,
                                     @Param("targetStatus") String targetStatus);

    // 【新增】为管理员查询所有商品的方法，支持按标题关键词搜索
    List<Product> findAllForAdmin(@Param("keyword") String keyword);

    long countTotalProducts();
    List<DailyStatsDTO> countProductTrends(@Param("days") int days);

    // 【新增】基于协同过滤的商品推荐查询
    List<Product> findRecommendedProducts(@Param("productId") String productId, @Param("limit") int limit);

    List<Product> findAvailableByCategory(@Param("categoryId") Integer categoryId,
                                          @Param("excludeId") String excludeId,
                                          @Param("limit") int limit);

    List<Product> findRecentProductsBySeller(@Param("sellerId") String sellerId,
                                             @Param("days") int days,
                                             @Param("limit") int limit,
                                             @Param("excludeId") String excludeId);

    List<Product> findBySeller(@Param("sellerId") String sellerId);

    List<Product> findProductsForEmbedding(@Param("offset") int offset, @Param("limit") int limit);

    Double getCategoryAveragePrice(@Param("categoryId") Integer categoryId);

    com.campus.trade.dto.CategoryPriceStats getCategoryPriceStats(@Param("categoryId") Integer categoryId);

    void deleteById(String productId);

}
