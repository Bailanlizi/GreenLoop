package com.campus.trade.service;

import com.campus.trade.dto.ProductDemandRequest;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductDemand;

import java.util.List;

public interface ProductDemandService {
    ProductDemand createDemand(ProductDemandRequest request, String userId);

    List<ProductDemand> getUserDemands(String userId);

    void deleteDemand(Long id, String userId);

    void notifyMatches(Product product);
}
