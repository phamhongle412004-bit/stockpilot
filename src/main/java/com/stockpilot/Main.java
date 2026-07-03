package com.stockpilot;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.InvalidInputException;
import com.stockpilot.model.*;
import com.stockpilot.service.OrderService;
import com.stockpilot.service.ReportService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        com.stockpilot.util.DatabaseConnection.initializeDatabase();
        try {
            org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            System.out.println(" H2 Console đã chạy tại: http://localhost:8082");
        } catch (Exception e) {
            System.out.println("Không thể khởi động H2 Console (Có thể nó đang chạy ẩn rồi): " + e.getMessage());
        }
        OrderService orderService = new OrderService();
        ReportService reportService = new ReportService();
        Scanner scanner = new Scanner(System.in);

        Customer mockCustomer = new Customer("Phạm Hồng Lê", "phl@gmail.com", "0912345678");
        mockCustomer.setId(1);

        System.out.println("   HỆ THỐNG QUẢN LÝ KHO HÀNG STOCKPILOT ");

        while (true) {
            System.out.println("\n--- MENU CHỨC NĂNG ---");
            System.out.println("1. Đặt hàng & Transaction ");
            System.out.println("2. Xem Báo cáo Doanh thu & Phân tích Stream ");
            System.out.println("3. Thoát chương trình");
            System.out.print("Chọn chức năng (1-3): ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    System.out.println("\n--- ĐẶT HÀNG (CHECKOUT) ---");
                    System.out.print("Nhập mã SKU sản phẩm muốn mua: ");
                    String sku = scanner.nextLine();
                    System.out.print("Nhập số lượng mua: ");
                    int qty = Integer.parseInt(scanner.nextLine());

                    Map<String, Integer> cart = new HashMap<>();
                    cart.put(sku, qty);

                    DiscountPolicy discountPolicy = (ord) -> BigDecimal.ZERO;
                    try {
                        System.out.println("Đang thực hiện checkout và lưu database qua Transaction...");
                        Order order = orderService.checkout(mockCustomer, cart, discountPolicy);
                        System.out.println("Đặt hàng THÀNH CÔNG!");
                        System.out.println(order.toString());
                    } catch (InsufficientStockException e) {
                        System.out.println("LỖI NGHIỆP VỤ: " + e.getMessage());
                        System.out.println("-> Hệ thống đã tự động ROLLBACK, không trừ kho lỗi.");
                    } catch (InvalidInputException e) {
                        System.out.println("LỖI DỮ LIỆU: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("LỖI HỆ THỐNG: " + e.getMessage());
                    }
                    break;

                case "2":
                    System.out.println("\n--- BÁO CÁO DOANH THU & PHÂN TÍCH ---");
                    try {
                        java.time.LocalDateTime end = java.time.LocalDateTime.now();
                        java.time.LocalDateTime start = end.minusMonths(1);
                        System.out.println("1. Tổng doanh thu hệ thống (1 tháng qua): " + reportService.calculateTotalRevenue(start, end) + " VND");
                        System.out.println("2. Tổng số đơn hàng (1 tháng qua): " + reportService.getTotalOrdersCount(start, end));

                        System.out.println("3. Doanh thu theo danh mục sản phẩm: " + reportService.getRevenueByCategory());
                        System.out.println("4. Top sản phẩm bán chạy: " + reportService.getTopSellingProducts(3));
                    } catch (Exception e) {
                        System.out.println(" Không thể tải báo cáo. Hãy đảm bảo Database đã có dữ liệu đơn hàng: " + e.getMessage());
                    }
                    break;

                case "3":
                    System.out.println("Tạm biệt!");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Lựa chọn không hợp lệ, vui lòng chọn lại!");
            }
        }
    }
}