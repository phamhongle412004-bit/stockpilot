package com.stockpilot.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private Customer customer;
    private LocalDateTime orderDate;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    public Order(Customer customer) {
        this.customer = customer;
        this.orderDate = LocalDateTime.now();
    }
    public void addItem(OrderItem item) {
        this.items.add(item);
        this.totalAmount = this.totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    @Override
    public String toString() {
        return String.format("Order[ID=%d, Customer=%s, Total=%s, Discount=%s, Final=%s, ItemsCount=%d]",
                id, customer.getName(), totalAmount, discountAmount, finalAmount, items.size());
    }
}