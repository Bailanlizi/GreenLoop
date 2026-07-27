package com.campus.trade.service.impl;

import com.campus.trade.dto.ProductDemandRequest;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductDemand;
import com.campus.trade.mapper.ProductDemandMapper;
import com.campus.trade.service.NotificationService;
import com.campus.trade.service.ProductDemandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductDemandServiceImpl implements ProductDemandService {

    private final ProductDemandMapper demandMapper;
    private final NotificationService notificationService;

    public ProductDemandServiceImpl(ProductDemandMapper demandMapper, NotificationService notificationService) {
        this.demandMapper = demandMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ProductDemand createDemand(ProductDemandRequest request, String userId) {
        ProductDemand demand = new ProductDemand();
        demand.setUserId(userId);
        demand.setCategoryId(request.getCategoryId());
        demand.setKeyword(trimToNull(request.getKeyword()));
        demand.setMinPrice(request.getMinPrice());
        demand.setMaxPrice(request.getMaxPrice());
        demand.setConditionLevel(request.getConditionLevel());
        if (request.getDeliveryOptions() != null && !request.getDeliveryOptions().isEmpty()) {
            demand.setDeliveryOptions(String.join(",", request.getDeliveryOptions()));
        }
        demand.setStatus("ACTIVE");
        demandMapper.insert(demand);
        return demand;
    }

    @Override
    public List<ProductDemand> getUserDemands(String userId) {
        return demandMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteDemand(Long id, String userId) {
        demandMapper.deleteByIdAndUserId(id, userId);
    }

    @Override
    public void notifyMatches(Product product) {
        if (product == null) {
            return;
        }
        String deliveryOptions = Optional.ofNullable(product.getDeliveryOptions()).orElse("");
        List<ProductDemand> matches = demandMapper.findMatchingDemands(
                product.getSellerId(),
                product.getCategoryId(),
                Optional.ofNullable(product.getTitle()).orElse(""),
                Optional.ofNullable(product.getDescription()).orElse(""),
                product.getPrice(),
                product.getConditionLevel(),
                deliveryOptions
        );
        if (matches == null || matches.isEmpty()) {
            return;
        }
        for (ProductDemand demand : matches) {
            String content = "您关注的需求已匹配到新商品：" + product.getTitle();
            notificationService.createNotification(demand.getUserId(), "DEMAND_MATCH", content, product.getId());
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
