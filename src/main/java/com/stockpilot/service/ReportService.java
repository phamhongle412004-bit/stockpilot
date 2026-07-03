package com.stockpilot.service;

import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class ReportService {
    private final OrderRepository orderRepository = new OrderRepository();
    private final ProductRepository productRepository = new ProductRepository();

    public BigDecimal calculateTotalRevenue(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAll().stream()
                .map(order -> (Order) order)
                .filter(order -> !order.getOrderDate().isBefore(start) && !order.getOrderDate().isAfter(end))
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, (sum, amount) -> sum.add(amount));
    }

    public long getTotalOrdersCount(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAll().stream()
                .map(order -> (Order) order)
                .filter(order -> !order.getOrderDate().isBefore(start) && !order.getOrderDate().isAfter(end))
                .count();
    }
    public Map<String, BigDecimal> getRevenueByCategory() {
        return orderRepository.findAll().stream()
                .map(order -> (Order) order)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> ((OrderItem) item).getProduct().getCategory(),
                        Collectors.mapping(
                                item -> {
                                    OrderItem orderItem = (OrderItem) item;
                                    return orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                                },
                                Collectors.reducing(BigDecimal.ZERO, (sum, price) -> sum.add(price))
                        )
                ));
    }
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findAll().stream()
                .map(product -> (Product) product)
                .filter(product -> product.getStockQuantity() < threshold)
                .collect(Collectors.toList());
    }
    public Map<String, Integer> getTopSellingProducts(int topN) {
        return orderRepository.findAll().stream()
                .map(order -> (Order) order)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> ((OrderItem) item).getProduct().getName(),
                        Collectors.summingInt(item -> ((OrderItem) item).getQuantity())
                ))
                .entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .limit(topN)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }
}