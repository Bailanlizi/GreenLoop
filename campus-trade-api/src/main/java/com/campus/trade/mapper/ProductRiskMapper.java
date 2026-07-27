package com.campus.trade.mapper;

import com.campus.trade.entity.ProductRisk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductRiskMapper {
    void insert(ProductRisk productRisk);
}
