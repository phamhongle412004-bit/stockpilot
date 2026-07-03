package com.stockpilot.service;

import com.stockpilot.model.Order;
import java.math.BigDecimal;

@FunctionalInterface
public interface PricingRule {
    BigDecimal applyRule(Order order);
}