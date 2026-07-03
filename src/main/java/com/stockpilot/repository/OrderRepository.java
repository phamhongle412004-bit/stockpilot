package com.stockpilot.repository;

import com.stockpilot.exception.DataAccessException;
import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.util.DatabaseConnection;
import java.sql.*;
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
    public List<Order> findAll() { return List.of(); }
    @Override
    public void update(Order order) {}
    @Override
    public void deleteById(Integer id) {}
}