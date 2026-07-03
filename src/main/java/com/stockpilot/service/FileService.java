package com.stockpilot.service;

import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.exception.FileStorageException;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileService {
    private final ProductRepository productRepository = new ProductRepository();
    private static final String SKU_REGEX = "^[A-Z]{2}\\d{3}$";
    public void importProductsFromCsv(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileStorageException("Lỗi: Tập tin CSV không tồn tại tại: " + filePath);
        }
        int successCount = 0;
        int failureCount = 0;
        int lineNumber = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String header = br.readLine();
            lineNumber++;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (tokens.length < 5) {
                    System.err.println("⚠️ Dòng " + lineNumber + " lỗi cấu trúc (thiếu cột): " + line);
                    failureCount++;
                    continue;
                }
                try {
                    String sku = tokens[0].replace("\"", "").trim();
                    String name = tokens[1].replace("\"", "").trim();
                    String category = tokens[2].replace("\"", "").trim();
                    String priceStr = tokens[3].replace("\"", "").trim();
                    String qtyStr = tokens[4].replace("\"", "").trim();

                    BigDecimal price = new BigDecimal(priceStr);
                    int stockQty = Integer.parseInt(qtyStr);

                    if (!sku.matches(SKU_REGEX)) {
                        System.err.println(" Dòng " + lineNumber + " bị từ chối: SKU '" + sku + "' sai định dạng.");
                        failureCount++;
                        continue;
                    }
                    Product product = new Product(sku, name, category, price, stockQty);
                    if (productRepository.findBySku(sku).isPresent()) {
                        System.out.println(" Dòng " + lineNumber + ": Sản phẩm " + sku + " đã tồn tại trong DB -> Bỏ qua.");
                        continue;
                    }
                    productRepository.save(product);
                    successCount++;

                } catch (Exception e) {
                    System.err.println(" Dòng " + lineNumber + " chứa kiểu dữ liệu sai quy cách: " + e.getMessage());
                    failureCount++;
                }
            }
            System.out.println("\n KẾT QUẢ TIẾN TRÌNH IMPORT TỪ FILE CSV:");
            System.out.println("   - Thành công đưa vào DB: " + successCount + " sản phẩm.");
            System.out.println("   - Bị lỗi cấu trúc dòng:   " + failureCount + " sản phẩm.");
        } catch (IOException e) {
            throw new FileStorageException("Lỗi hệ thống khi đang đọc tệp dữ liệu CSV", e);
        }
    }
    public void exportInvoice(Order order) {
        ensureOutputDirectoryExists();
        String fileName = "output/INVOICE_" + order.getId() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("      HOÁ ĐƠN BÁN HÀNG - STOCKPILOT     \n");
            writer.write("Mã hoá đơn : " + order.getId() + "\n");
            writer.write("Ngày mua   : " + order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Khách hàng : " + order.getCustomer().getName() + "\n");
            writer.write("--------------------------------------------------\n");
            writer.write(String.format("%-10s %-18s %-5s %-12s\n", "Mã SKU", "Tên Sản Phẩm", "SL", "Thành Tiền"));
            writer.write("--------------------------------------------------\n");

            for (OrderItem item : order.getItems()) {
                writer.write(String.format("%-10s %-18s %-5d %-12s\n",
                        item.getProduct().getSku(),
                        item.getProduct().getName().length() > 16 ? item.getProduct().getName().substring(0, 14) + ".." : item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice().multiply(new BigDecimal(item.getQuantity())) + "đ"
                ));
            }

            writer.write("--------------------------------------------------\n");
            writer.write(String.format("Tổng tiền tạm tính  : %s VND\n", order.getTotalAmount()));
            writer.write(String.format("Số tiền giảm trừ    : %s VND\n", order.getDiscountAmount()));
            writer.write(String.format("SỐ TIỀN THANH TOÁN : %s VND\n", order.getFinalAmount()));
            writer.write("==================================================\n");

            System.out.println(" Đã kết xuất hóa đơn thành công ra file: " + fileName);
        } catch (IOException e) {
            throw new FileStorageException("Lỗi khi ghi tệp hoá đơn", e);
        }
    }
    public void exportSalesReport(LocalDateTime start, LocalDateTime end, BigDecimal revenue, long ordersCount, String categoryStats) {
        ensureOutputDirectoryExists();
        String fileName = "output/SALES_REPORT_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("==================================================\n");
            writer.write("         BÁO CÁO THỐNG KÊ DOANH THU HỆ THỐNG      \n");
            writer.write("==================================================\n");
            writer.write("Thời gian lập báo cáo: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Khoảng phân tích từ  : " + start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " -> " + end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "\n");
            writer.write("--------------------------------------------------\n");
            writer.write("1. Tổng doanh thu thực nhận : " + revenue + " VND\n");
            writer.write("2. Tổng số thương vụ chốt   : " + ordersCount + " đơn hàng\n");
            writer.write("--------------------------------------------------\n");
            writer.write("3. Chi tiết dòng tiền theo danh mục ngành hàng:\n");
            writer.write(categoryStats + "\n");
            writer.write("==================================================\n");

            System.out.println(" Đã kết xuất file báo cáo phân tích tổng hợp tại: " + fileName);
        } catch (IOException e) {
            throw new FileStorageException("Lỗi khi ghi tệp báo cáo doanh thu", e);
        }
    }

    private void ensureOutputDirectoryExists() {
        try {
            Path path = Paths.get("output");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new FileStorageException("Không thể khởi tạo thư mục lưu trữ output/", e);
        }
    }
}