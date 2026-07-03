package com.stockpilot.model;
import java.math.BigDecimal;

public class OrderItem {
    private int id;
    private int orderId;
    private Product product;
    private int quantity;
    private BigDecimal price;

    public OrderItem(Product product, int quantity, BigDecimal price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }
    public OrderItem(int id, int orderId, Product product, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    @Override
    public String toString() {
        return String.format("OrderItem[Product=%s, Qty=%d, Price=%s]", product.getName(), quantity, price);
    }
}