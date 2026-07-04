package com.stockpilot;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.InvalidInputException;
import com.stockpilot.model.*;
import com.stockpilot.service.*;

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
            System.out.println("Không thể khởi động H2 Console: " + e.getMessage());
        }

        OrderService orderService = new OrderService();
        ReportService reportService = new ReportService();
        FileService fileService = new FileService();
        FlashSaleService flashSaleService = new FlashSaleService();
        BackgroundScheduler backgroundScheduler = new BackgroundScheduler();
        backgroundScheduler.startAutoExportTask();

        Scanner scanner = new Scanner(System.in);
        Customer mockCustomer = new Customer("Phạm Hồng Lê", "phl@gmail.com", "0912345678");
        mockCustomer.setId(1);

        System.out.println("   HỆ THỐNG QUẢN LÝ KHO HÀNG STOCKPILOT ");

        while (true) {
            System.out.println("\n--- MENU CHỨC NĂNG ---");
            System.out.println("1. Đặt hàng & Transaction");
            System.out.println("2. Xem Báo cáo Doanh thu & Phân tích Stream");
            System.out.println("3. Nhập danh mục sản phẩm từ file CSV");
            System.out.println("4. Giả lập Flash Sale đồng thời");
            System.out.println("5. Thoát chương trình");
            System.out.print("Chọn chức năng (1-5): ");

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
                    try {
                        Order order = orderService.checkout(mockCustomer, cart, ord -> BigDecimal.ZERO);
                        System.out.println("Đặt hàng THÀNH CÔNG!\n" + order);
                        fileService.exportInvoice(order);
                    } catch (Exception e) {
                        System.out.println("LỖI: " + e.getMessage());
                    }
                    break;

                case "2":
                    try {
                        java.time.LocalDateTime end = java.time.LocalDateTime.now();
                        java.time.LocalDateTime start = end.minusMonths(1);
                        BigDecimal totalRevenue = reportService.calculateTotalRevenue(start, end);
                        long totalOrders = reportService.getTotalOrdersCount(start, end);
                        String categoryStats = reportService.getRevenueByCategory().toString();

                        System.out.println("1. Tổng doanh thu hệ thống: " + totalRevenue + " VND");
                        System.out.println("2. Tổng số đơn hàng: " + totalOrders);
                        System.out.println("3. Doanh thu theo danh mục: " + categoryStats);

                        fileService.exportSalesReport(start, end, totalRevenue, totalOrders, categoryStats);
                    } catch (Exception e) {
                        System.out.println("Không thể tải báo cáo: " + e.getMessage());
                    }
                    break;

                case "3":
                    System.out.println("\n--- TIẾN TRÌNH IMPORT DANH MỤC TỪ FILE CSV ---");
                    System.out.print("Nhập tên file hoặc đường dẫn file CSV (Ví dụ: products.csv): ");
                    String pathCsv = scanner.nextLine();
                    try { fileService.importProductsFromCsv(pathCsv); } catch (Exception e) { System.out.println("❌ Lỗi: " + e.getMessage()); }
                    break;

                case "4":
                    System.out.println("\n--- GIẢ LẬP KỊCH BẢN TRANH CHẤP CONCURRENCY ---");
                    System.out.println("Sản phẩm thử nghiệm: 'SP004' (đang có tồn kho giới hạn là 10 sản phẩm)");
                    System.out.println("Hệ thống sẽ tung ra 20 yêu cầu mua hàng đồng thời!");
                    System.out.println("1. Chạy kịch bản UNSAFE (Không đồng bộ -> Gây lỗi bán lố kho)");
                    System.out.println("2. Chạy kịch bản SAFE (Có synchronized -> Đảm bảo an toàn kho)");
                    System.out.print("Chọn phương án chạy (1-2): ");
                    String subChoice = scanner.nextLine();
                    if ("1".equals(subChoice)) {
                        flashSaleService.runUnsafeFlashSale("SP004", 20);
                    } else if ("2".equals(subChoice)) {
                        flashSaleService.runSafeFlashSale("SP004", 20);
                    } else {
                        System.out.println("Lựa chọn không hợp lệ!");
                    }
                    break;

                case "5":
                    System.out.println("Đang tắt hệ thống...");
                    backgroundScheduler.stopScheduler();
                    System.out.println("Tạm biệt!");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Lựa chọn không hợp lệ, vui lòng chọn lại!");
            }
        }
    }
}