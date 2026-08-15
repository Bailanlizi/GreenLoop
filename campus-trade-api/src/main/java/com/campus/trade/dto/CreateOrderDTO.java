package com.campus.trade.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class CreateOrderDTO {
    @NotBlank(message = "商品不能为空")
    private String productId;

    @NotBlank(message = "履约方式不能为空")
    @Pattern(regexp = "MEETUP|SHIPPING", message = "履约方式不合法")
    private String deliveryMethod;

    private Integer meetupLocationId;
    private Long shippingAddressId;

    // Getters and Setters...
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    public Integer getMeetupLocationId() { return meetupLocationId; }
    public void setMeetupLocationId(Integer meetupLocationId) { this.meetupLocationId = meetupLocationId; }
    public Long getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(Long shippingAddressId) { this.shippingAddressId = shippingAddressId; }
}
