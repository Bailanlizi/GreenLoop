package com.campus.trade.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    AWAITING_MEETUP,
    AWAITING_SHIPMENT,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    public boolean canBuyerCancel() {
        return this == PENDING_PAYMENT || this == AWAITING_MEETUP || this == AWAITING_SHIPMENT;
    }

    public boolean canBuyerConfirmCompletion(String deliveryMethod) {
        return ("MEETUP".equals(deliveryMethod) && this == AWAITING_MEETUP)
                || ("SHIPPING".equals(deliveryMethod) && this == SHIPPED);
    }
}
