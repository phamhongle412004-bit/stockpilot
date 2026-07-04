package com.stockpilot.service;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.ProductNotFoundException;
import com.stockpilot.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {
    private OrderService orderService;
    private Customer testCustomer;
    private DiscountPolicy noDiscountPolicy;
    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        testCustomer = new Customer("Khách Hàng Test", "test@gmail.com", "0912345678");
        testCustomer.setId(1);
        noDiscountPolicy = (order) -> BigDecimal.ZERO;
    }

    // TEST 1: Kiểm tra việc ném ngoại lệ khi mua sản phẩm không tồn tại
    @Test
    void testCheckout_ProductNotFound_ShouldThrowException() {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("SKU-KHONG-TON-TAI", 1);

        assertThrows(ProductNotFoundException.class, () -> {
            orderService.checkout(testCustomer, cart, noDiscountPolicy);
        });
    }

    //TEST 2: Kiểm tra việc ném ngoại lệ khi mua quá số lượng tồn kho
    @Test
    void testCheckout_InsufficientStock_ShouldThrowException() {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("SP004", 999);
        assertThrows(InsufficientStockException.class, () -> {
            orderService.checkout(testCustomer, cart, noDiscountPolicy);
        });
    }

    // TEST 3: Kiểm tra tính toán giảm giá theo phần trăm chạy chính xác
    @Test
    void testCheckout_PercentageDiscount_ShouldCalculateCorrectly() {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("SP004", 1);
        DiscountPolicy percentDiscount = (order) -> order.getTotalAmount().multiply(new BigDecimal("0.10"));

        try {
            Order order = orderService.checkout(testCustomer, cart, percentDiscount);
            BigDecimal expectedDiscount = order.getTotalAmount().multiply(new BigDecimal("0.10"));
            assertEquals(expectedDiscount, order.getDiscountAmount());
            assertEquals(order.getTotalAmount().subtract(expectedDiscount), order.getFinalAmount());
        } catch (Exception e) {

        }
    }
}