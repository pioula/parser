package com.pu429640;

import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MySqlWriter {
    private static final Logger logger = LoggerFactory.getLogger(MySqlWriter.class);
    private final String url;
    private final String user;
    private final String password;
    private final String tableName;
    private final ConcurrentLinkedQueue<AggregationEntry> batchEntries;
    private static final int BATCH_SIZE = 10000;
    private static final int FLUSH_INTERVAL_SECONDS = 5;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock writeLock;

    public MySqlWriter(String url, String user, String password, String tableName) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.tableName = tableName;
        this.batchEntries = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.writeLock = new ReentrantLock();

        createTableIfNotExists();
        schedulePeriodicFlush();
    }

    private void createTableIfNotExists() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "timestamp TIMESTAMP, " +
                    "action VARCHAR(50), " +
                    "origin VARCHAR(50), " +
                    "brand_id VARCHAR(50), " +
                    "category_id VARCHAR(50), " +
                    "count BIGINT, " +
                    "sum_price BIGINT, " +
                    "PRIMARY KEY (timestamp, action, origin, brand_id, category_id)" +
                    ") ENGINE=InnoDB";
            try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
                stmt.execute();
            }
        }
    }

    private void schedulePeriodicFlush() {
        scheduler.scheduleAtFixedRate(this::flushBatch, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void writeAggregation(Windowed<String> windowedKey, Aggregation aggregation) {
        String[] keyParts = windowedKey.key().split(":");
        
        if (keyParts.length < 4) {
            logger.error("Invalid key format. Expected at least 4 parts, but got: {}. Key: {}", keyParts.length, windowedKey.key());
            return;
        }

        String action = keyParts[0];
        String origin = keyParts[1];
        String brandId = keyParts[2];
        String categoryId = keyParts[3];

        AggregationEntry entry = new AggregationEntry(
            Instant.ofEpochMilli(windowedKey.window().startTime().toEpochMilli()),
            action,
            origin,
            brandId,
            categoryId,
            aggregation.getCount(),
            aggregation.getSumPrice()
        );

        batchEntries.offer(entry);

        if (batchEntries.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    private void flushBatch() {
        List<AggregationEntry> entriesAsList = new ArrayList<>();
        AggregationEntry entry;
        while ((entry = batchEntries.poll()) != null) {
            entriesAsList.add(entry);
        }

        if (entriesAsList.isEmpty()) {
            return;
        }

        writeLock.lock();
        try {
            executeBatchWrite(entriesAsList);
        } finally {
            writeLock.unlock();
        }
    }

    private void executeBatchWrite(List<AggregationEntry> entries) {
        String insertSQL = "INSERT INTO " + tableName + " (timestamp, action, origin, brand_id, category_id, count, sum_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE count = VALUES(count), sum_price = VALUES(sum_price)";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {

            connection.setAutoCommit(false);

            for (AggregationEntry entry : entries) {
                pstmt.setTimestamp(1, java.sql.Timestamp.from(entry.timestamp));
                pstmt.setString(2, entry.action);
                pstmt.setString(3, entry.origin);
                pstmt.setString(4, entry.brandId);
                pstmt.setString(5, entry.categoryId);
                pstmt.setLong(6, entry.count);
                pstmt.setLong(7, entry.sumPrice);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            connection.commit();
            logger.info("Batch write of {} entries completed successfully", entries.size());
        } catch (SQLException e) {
            logger.error("Error executing batch write to MySQL", e);
            throw new RuntimeException("Error executing batch write to MySQL", e);
        }
    }

    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        flushBatch(); // Flush any remaining entries
        logger.info("MySqlWriter closed");
    }

    private static class AggregationEntry {
        final Instant timestamp;
        final String action;
        final String origin;
        final String brandId;
        final String categoryId;
        final long count;
        final long sumPrice;

        AggregationEntry(Instant timestamp, String action, String origin, String brandId, String categoryId, long count, long sumPrice) {
            this.timestamp = timestamp;
            this.action = action;
            this.origin = origin;
            this.brandId = brandId;
            this.categoryId = categoryId;
            this.count = count;
            this.sumPrice = sumPrice;
        }
    }
}