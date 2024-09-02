package com.pu429640;

import org.apache.kafka.streams.kstream.Windowed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public class PostgresWriter {
    private final Connection connection;
    private final String tableName;

    public PostgresWriter(String url, String user, String password, String tableName) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
        this.tableName = tableName;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "timestamp TIMESTAMP WITH TIME ZONE, " +
                "action VARCHAR(50), " +
                "origin VARCHAR(50), " +
                "brand_id VARCHAR(50), " +
                "category_id VARCHAR(50), " +
                "count BIGINT, " +
                "sum_price BIGINT, " +
                "PRIMARY KEY (timestamp, action, origin, brand_id, category_id)" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.execute();
        }
    }

    public void writeAggregation(Windowed<String> windowedKey, Aggregation aggregation) {
        String[] keyParts = windowedKey.key().split(":");
        String action = keyParts[0];
        String origin = keyParts[1];
        String brandId = keyParts[2];
        String categoryId = keyParts[3];

        String insertSQL = "INSERT INTO " + tableName + " (timestamp, action, origin, brand_id, category_id, count, sum_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.from(Instant.ofEpochMilli(windowedKey.window().startTime().toEpochMilli())));
            pstmt.setString(2, action);
            pstmt.setString(3, origin);
            pstmt.setString(4, brandId);
            pstmt.setString(5, categoryId);
            pstmt.setLong(6, aggregation.getCount());
            pstmt.setLong(7, aggregation.getSumPrice());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error writing aggregation to PostgreSQL", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error closing PostgreSQL connection", e);
        }
    }
}