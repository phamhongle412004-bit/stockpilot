package com.stockpilot.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:h2:./stockpilot_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            System.out.println(" Đang kiểm tra và khởi tạo Database cấu trúc H2...");

            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "email VARCHAR(100), " +
                    "phone VARCHAR(20))");

            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sku VARCHAR(50) UNIQUE, " +
                    "name VARCHAR(100), " +
                    "category VARCHAR(50), " +
                    "price DECIMAL(15,2), " +
                    "stock_quantity INT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_id INT, " +
                    "total_amount DECIMAL(15,2), " +
                    "discount_amount DECIMAL(15,2), " +
                    "final_amount DECIMAL(15,2), " +
                    "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "order_id INT, " +
                    "product_id INT, " +
                    "quantity INT, " +
                    "price DECIMAL(15,2), " +
                    "FOREIGN KEY (order_id) REFERENCES orders(id), " +
                    "FOREIGN KEY (product_id) REFERENCES products(id))");

            stmt.execute("MERGE INTO customers KEY(id) VALUES (1, 'Phạm Hồng Lê', 'phl@gmail.com', '0912345678')");
            stmt.execute("MERGE INTO products KEY(sku) VALUES (1, 'SP001', 'Học Java', 'Gaming', 120000.00, 50)");
            stmt.execute("MERGE INTO products KEY(sku) VALUES (2, 'SP002', 'Tiếng Nhật N3', 'Gaming', 250000.00, 30)");
            stmt.execute("MERGE INTO products KEY(sku) VALUES (3, 'SP003', 'Bút Deli', 'Office', 15000.00, 5)");

            System.out.println(" Database đã sẵn sàng! Dữ liệu mẫu đã được nạp.");

        } catch (SQLException e) {
            System.err.println(" Lỗi khi khởi tạo cấu trúc Database mẫu: " + e.getMessage());
        }
    }
}