package com.pu429640;

import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MySqlWriter {
    private static final Logger logger = LoggerFactory.getLogger(MySqlWriter.class);
    private final String url;
    private final String user;
    private final String password;
    private final String tableName;
    private final ConcurrentLinkedQueue<AggregationEntry> batchEntries;
   private static final int BATCH_SIZE = 30000;
   private static final int FLUSH_INTERVAL_SECONDS = 5;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService flushExecutor;
    private final AtomicInteger activeFlushCount;

    public MySqlWriter(String url, String user, String password, String tableName) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.tableName = tableName;
        this.batchEntries = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(1);

        createTableIfNotExists();
        schedulePeriodicFlush();
        this.flushExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.activeFlushCount = new AtomicInteger(0);
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
        Instant currentWindowStart = Instant.ofEpochMilli(windowedKey.window().startTime().toEpochMilli());

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
                currentWindowStart,
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

        activeFlushCount.incrementAndGet();
        flushExecutor.submit(() -> {
            try {
                Instant beforeFlush = Instant.now();
                executeBatchWrite(entriesAsList);
                Instant afterFlush = Instant.now();

                Duration flushDuration = Duration.between(beforeFlush, afterFlush);
                logger.info("Time to write window data: {} seconds", flushDuration.toSeconds());
            } finally {
                activeFlushCount.decrementAndGet();
            }
        });
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
        flushExecutor.shutdown();
        try {
            // Wait for all ongoing flushes to complete
            while (activeFlushCount.get() > 0) {
                if (!flushExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.info("Waiting for {} active flushes to complete...", activeFlushCount.get());
                }
            }
        } catch (InterruptedException e) {
            flushExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

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