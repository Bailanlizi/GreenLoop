package com.campus.trade.mapper;

import com.campus.trade.entity.ProductDemand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductDemandMapper {
    void insert(ProductDemand demand);

    List<ProductDemand> findByUserId(@Param("userId") String userId);

    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    List<ProductDemand> findMatchingDemands(@Param("sellerId") String sellerId,
                                            @Param("categoryId") Integer categoryId,
                                            @Param("title") String title,
                                            @Param("description") String description,
                                            @Param("price") java.math.BigDecimal price,
                                            @Param("conditionLevel") Integer conditionLevel,
                                            @Param("deliveryOptions") String deliveryOptions);
}
