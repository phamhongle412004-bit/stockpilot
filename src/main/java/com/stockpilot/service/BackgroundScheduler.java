package com.stockpilot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ReportService reportService = new ReportService();
    private final FileService fileService = new FileService();
    public void startAutoExportTask() {
        System.out.println(" Luồng chạy ngầm tự động xuất Báo cáo (Schedule Task) đã kích hoạt.");
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime end = LocalDateTime.now();
                LocalDateTime start = end.minusMonths(1);

                BigDecimal totalRevenue = reportService.calculateTotalRevenue(start, end);
                long totalOrders = reportService.getTotalOrdersCount(start, end);
                String categoryStats = reportService.getRevenueByCategory().toString();

                System.out.println("\n[BACKGROUND THREAD] Tự động lưu snapshot hệ thống...");
                fileService.exportSalesReport(start, end, totalRevenue, totalOrders, "Snapshot: " + categoryStats);
            } catch (Exception e) {
                System.err.println("[BACKGROUND THREAD]  Gặp lỗi khi tự lưu báo cáo: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);//5p luồng chạy ngầm chạy lại 1 lần
    }
    public void stopScheduler() {
        System.out.println(" Đang tắt luồng chạy ngầm an toàn (Graceful Shutdown)...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.MINUTES)) {
                scheduler.shutdownNow();
            }
            System.out.println(" Luồng chạy ngầm đã dừng hẳn.");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}