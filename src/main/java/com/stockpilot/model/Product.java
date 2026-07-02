package com.stockpilot.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

public class Product {
    private int id;
    private String sku;
    private String name;
    private String category;
    private BigDecimal price; // Sử dụng BigDecimal cho tiền tệ theo tiêu chuẩn nâng cao
    private int stockQuantity;

    // Biểu thức chính quy (Regex) để bắt buộc định dạng SKU (VD: ABC-1234)
    private static final String SKU_REGEX = "^[A-Z]{3}-\\d{4}$";
    private static final Pattern SKU_PATTERN = Pattern.compile(SKU_REGEX);

    // Constructor dùng khi tạo mới sản phẩm (chưa có ID từ Database)
    public Product(String sku, String name, String category, BigDecimal price, int stockQuantity) {
        setSku(sku); // Kiểm tra Regex ngay khi truyền vào
        this.name = name;
        this.category = category;
        this.price = price;
        setStockQuantity(stockQuantity); // Kiểm tra số lượng không âm
    }

    // Constructor dùng khi đọc dữ liệu từ Database ra (đã có ID)
    public Product(int id, String sku, String name, String category, BigDecimal price, int stockQuantity) {
        this.id = id;
        setSku(sku);
        this.name = name;
        this.category = category;
        this.price = price;
        setStockQuantity(stockQuantity);
    }

    // Hàm tiện ích để kiểm tra tính hợp lệ của SKU bằng Regex
    public static boolean isValidSku(String sku) {
        if (sku == null) return false;
        return SKU_PATTERN.matcher(sku).matches();
    }

    // --- Getters và Setters có ràng buộc dữ liệu (Encapsulation) ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) {
        if (!isValidSku(sku)) {
            throw new IllegalArgumentException("Định dạng SKU không hợp lệ! Phải có dạng 3 chữ hoa - 4 chữ số (VD: ABC-1234).");
        }
        this.sku = sku;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được nhỏ hơn 0.");
        }
        this.stockQuantity = stockQuantity;
    }

    // Ghi đè equals và hashCode dựa trên SKU để phục vụ so sánh dữ liệu sau này
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    // Ghi đè toString để hiển thị đẹp mắt thông tin sản phẩm trên giao diện CLI Console
    @Override
    public String toString() {
        return String.format("Product[ID=%d, SKU='%s', Name='%s', Category='%s', Price=%s, Stock=%d]",
                id, sku, name, category, price, stockQuantity);
    }
}