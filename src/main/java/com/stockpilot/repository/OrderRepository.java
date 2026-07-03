package com.stockpilot.repository;

import com.stockpilot.exception.DataAccessException;
import com.stockpilot.model.Customer;
import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderRepository implements Repository<Order, Integer> {
    @Override
    public void save(Order order) {
        String insertOrderSql = "INSERT INTO orders (customer_id, total_amount, discount_amount, final_amount) VALUES (?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setInt(1, order.getCustomer().getId());
                orderStmt.setBigDecimal(2, order.getTotalAmount());
                orderStmt.setBigDecimal(3, order.getDiscountAmount());
                orderStmt.setBigDecimal(4, order.getFinalAmount());
                orderStmt.executeUpdate();
                try (ResultSet generatedKeys = orderStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        order.setId(generatedKeys.getInt(1));
                    }
                }
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql);
                 PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {

                for (OrderItem item : order.getItems()) {
                    itemStmt.setInt(1, order.getId());
                    itemStmt.setInt(2, item.getProduct().getId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setBigDecimal(4, item.getPrice());
                    itemStmt.addBatch();

                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getProduct().getId());
                    stockStmt.addBatch();
                }
                itemStmt.executeBatch();
                stockStmt.executeBatch();
            }
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw new DataAccessException("Lỗi nghiêm trọng khi rollback transaction đơn hàng", rollbackEx);
                }
            }
            throw new DataAccessException("Lỗi hệ thống khi xử lý transaction đặt hàng. Đã thực hiện rollback an toàn.", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Không thể đóng kết nối database: " + e.getMessage());
                }
            }
        }
    }
    @Override
    public Optional<Order> findById(Integer id) { return Optional.empty(); }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, c.name AS customer_name, c.email AS customer_email, c.phone AS customer_phone " +
                "FROM orders o JOIN customers c ON o.customer_id = c.id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getString("customer_name"),
                        rs.getString("customer_email"),
                        rs.getString("customer_phone")
                );
                customer.setId(rs.getInt("customer_id"));

                Order order = new Order(customer);
                order.setId(rs.getInt("id"));
                order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
                order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                order.setFinalAmount(rs.getBigDecimal("final_amount"));
                loadOrderItems(conn, order);
                orders.add(order);
            }
        } catch (SQLException e) {
            throw new com.stockpilot.exception.DataAccessException("Lỗi khi tải toàn bộ danh sách đơn hàng để làm báo cáo", e);
        }
        return orders;
    }
    private void loadOrderItems(Connection conn, Order order) throws SQLException {
        String sql = "SELECT oi.*, p.sku, p.name AS product_name, p.category, p.stock_quantity " +
                "FROM order_items oi JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, order.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                            rs.getString("sku"),
                            rs.getString("product_name"),
                            rs.getString("category"),
                            rs.getBigDecimal("price"),
                            rs.getInt("stock_quantity")
                    );
                    product.setId(rs.getInt("product_id"));

                    OrderItem item = new OrderItem(
                            rs.getInt("id"),
                            rs.getInt("order_id"),
                            product,
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")
                    );
                    order.addItem(item);
                }
            }
        }
    }
    @Override
    public void update(Order order) {}
    @Override
    public void deleteById(Integer id) {}
}