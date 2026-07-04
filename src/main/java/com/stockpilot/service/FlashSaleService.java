package com.stockpilot.service;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.model.Customer;
import com.stockpilot.model.Order;
import com.stockpilot.service.OrderService;
import com.stockpilot.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FlashSaleService {
    private final OrderService orderService = new OrderService();
    private static final Object lock = new Object();

    private void prepareCustomersInDatabase(int total) {
        String sql = "MERGE INTO customers (id, name, email, phone) KEY(id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= total + 100; i++) {
                ps.setInt(1, i);
                ps.setString(2, "Khach Hang " + i);
                ps.setString(3, "user" + i + "@gmail.com");
                ps.setString(4, String.format("0912345%03d", i));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            System.err.println("️ Cảnh báo chuẩn bị dữ liệu khách hàng: " + e.getMessage());
        }
    }
    public void runUnsafeFlashSale(String productSku, int totalOrders) {
        System.out.println("\n BẮT ĐẦU FLASH SALE (UNSAFE) - KHÔNG ĐỒNG BỘ...");
        prepareCustomersInDatabase(totalOrders);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 1; i <= totalOrders; i++) {
            final int customerId = i;
            executor.execute(() -> {
                try {
                    String phone = String.format("0901234%03d", customerId);
                    Customer customer = new Customer("Khách hàng " + customerId, "customer" + customerId + "@gmail.com", phone);
                    customer.setId(customerId);

                    Map<String, Integer> cart = new HashMap<>();
                    cart.put(productSku, 1);

                    Order order = orderService.checkout(customer, cart, ord -> BigDecimal.ZERO);
                    System.out.println( customer.getName() + " mua thành công. Mã đơn: " + order.getId());
                } catch (InsufficientStockException e) {
                    System.err.println(" Khách hàng " + customerId + " thất bại: Hết hàng.");
                } catch (Exception e) {
                    System.err.println(" Lỗi luồng: " + e.getMessage());
                }
            });
        }
        shutdownExecutor(executor);
    }
    public void runSafeFlashSale(String productSku, int totalOrders) {
        System.out.println("\n BẮT ĐẦU FLASH SALE (SAFE) - ĐÃ ĐỒNG BỘ HÓA LUỒNG...");
        prepareCustomersInDatabase(totalOrders);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 1; i <= totalOrders; i++) {
            final int customerId = i;
            executor.execute(() -> {
                try {
                    String phone = String.format("0912345%03d", customerId);
                    Customer customer = new Customer("Khách hàng VIP " + customerId, "vip" + customerId + "@gmail.com", phone);
                    customer.setId(customerId);

                    Map<String, Integer> cart = new HashMap<>();
                    cart.put(productSku, 1);
                    Order order = orderService.checkout(customer, cart, ord -> BigDecimal.ZERO);
                    System.out.println("🛒 " + customer.getName() + " chốt đơn thành công! Mã đơn: " + order.getId());
                } catch (InsufficientStockException e) {
                    System.out.println(" Khách hàng VIP " + customerId + " bị chặn: Kho đã hết hàng sạch sẽ.");
                } catch (Exception e) {
                    System.err.println(" Lỗi luồng hệ thống: " + e.getMessage());
                }
            });
        }
        shutdownExecutor(executor);
    }
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}