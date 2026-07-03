package com.stockpilot.model;

import java.math.BigDecimal;

public interface DiscountPolicy {
    BigDecimal calculateDiscount(Order order);
}