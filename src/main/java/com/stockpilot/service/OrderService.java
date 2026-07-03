package com.stockpilot.service;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.ProductNotFoundException;
import com.stockpilot.model.*;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Map;

public class OrderService {
    private final ProductRepository productRepository = new ProductRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    public Order checkout(Customer customer, Map<String, Integer> cart, DiscountPolicy discountPolicy) {
        Order order = new Order(customer);
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String sku = entry.getKey();
            int requestedQty = entry.getValue();

            Product product = productRepository.findBySku(sku)
                    .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm có mã SKU: " + sku));
            if (product.getStockQuantity() < requestedQty) {
                throw new InsufficientStockException(String.format(
                        "Sản phẩm '%s' không đủ hàng trong kho! (Yêu cầu: %d, Hiện có: %d)",
                        product.getName(), requestedQty, product.getStockQuantity()
                ));
            }
            OrderItem item = new OrderItem(product, requestedQty, product.getPrice());
            order.addItem(item);
        }
        PricingRule pricingRule = (ord) -> {
            BigDecimal discount = discountPolicy.calculateDiscount(ord);
            if (discount.compareTo(ord.getTotalAmount()) > 0) {
                discount = ord.getTotalAmount();
            }
            ord.setDiscountAmount(discount);
            BigDecimal finalPrice = ord.getTotalAmount().subtract(discount);
            ord.setFinalAmount(finalPrice);
            return finalPrice;
        };
        pricingRule.applyRule(order);
        orderRepository.save(order);
        return order;
    }
}