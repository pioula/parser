package com.pu429640.services;

import com.pu429640.domain.Action;
import com.pu429640.domain.Aggregate;
import com.pu429640.domain.AggregatesQueryResult;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MySqlReader {
    private static final Logger logger = LoggerFactory.getLogger(MySqlReader.class);
    private final String url;
    private final String user;
    private final String password;
    private final String tableName;

    public MySqlReader(String url, String user, String password, String tableName) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.tableName = tableName;
    }

    public AggregatesQueryResult getAggregates(LocalDateTime timeFrom, LocalDateTime timeTo, Action action, List<Aggregate> aggregates, String origin, String brandId, String categoryId) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT DATE_FORMAT(timestamp, '%Y-%m-%dT%H:%i:00') as bucket, ");
        queryBuilder.append("action, ");
        if (origin != null)
            queryBuilder.append("origin, ");
        if (brandId != null)
            queryBuilder.append("brand_id, ");
        if (categoryId != null)
            queryBuilder.append("category_id, ");
        for (Aggregate aggregate : aggregates) {
            switch (aggregate) {
                case COUNT:
                    queryBuilder.append("SUM(count) as count, ");
                    break;
                case SUM_PRICE:
                    queryBuilder.append("SUM(sum_price) as sum_price, ");
                    break;
            }
        }

        queryBuilder.setLength(queryBuilder.length() - 2);
        queryBuilder.append(" FROM ").append(this.tableName);
        queryBuilder.append(" WHERE timestamp >= ? AND timestamp < ? AND action = ?");
        if (origin != null)
            queryBuilder.append(" AND origin = ?");
        if (brandId != null)
            queryBuilder.append(" AND brand_id = ?");
        if (categoryId != null)
            queryBuilder.append(" AND category_id = ?");
        queryBuilder.append(" GROUP BY bucket, action");
        if (origin != null)
            queryBuilder.append(", origin");
        if (brandId != null)
            queryBuilder.append(", brand_id");
        if (categoryId != null)
            queryBuilder.append(", category_id");
        queryBuilder.append(" ORDER BY bucket");
        try {
            Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
            try {
                PreparedStatement pstmt = connection.prepareStatement(queryBuilder.toString());
                try {
                    int paramIndex = 1;
                    pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(timeFrom));
                    pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(timeTo));
                    pstmt.setString(paramIndex++, action.toString());
                    if (origin != null)
                        pstmt.setString(paramIndex++, origin);
                    if (brandId != null)
                        pstmt.setString(paramIndex++, brandId);
                    if (categoryId != null)
                        pstmt.setString(paramIndex++, categoryId);
                    ResultSet rs = pstmt.executeQuery();
                    try {
                        AggregatesQueryResult aggregatesQueryResult = processResultSet(rs, action, aggregates, origin, brandId, categoryId, timeFrom, timeTo);
                        if (rs != null)
                            rs.close();
                        if (pstmt != null)
                            pstmt.close();
                        if (connection != null)
                            connection.close();
                        return aggregatesQueryResult;
                    } catch (Throwable throwable) {
                        if (rs != null)
                            try {
                                rs.close();
                            } catch (Throwable throwable1) {
                                throwable.addSuppressed(throwable1);
                            }
                        throw throwable;
                    }
                } catch (Throwable throwable) {
                    if (pstmt != null)
                        try {
                            pstmt.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                    throw throwable;
                }
            } catch (Throwable throwable) {
                if (connection != null)
                    try {
                        connection.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
            }
        } catch (SQLException e) {
            logger.error("Error executing query", e);
            throw new RuntimeException("Error executing query", e);
        }
    }

    private AggregatesQueryResult processResultSet(ResultSet rs, Action action, List<Aggregate> aggregates, String origin, String brandId, String categoryId, LocalDateTime timeFrom, LocalDateTime timeTo) throws SQLException {
        AggregatesQueryResult result = new AggregatesQueryResult();
        List<String> columns = new ArrayList<>();
        columns.add("1m_bucket");
        columns.add("action");
        if (origin != null)
            columns.add("origin");
        if (brandId != null)
            columns.add("brand_id");
        if (categoryId != null)
            columns.add("category_id");
        for (Aggregate aggregate : aggregates)
            columns.add(aggregate.toString().toLowerCase());
        result.setColumns(columns);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        List<List<String>> rows = new ArrayList<>();
        LocalDateTime current = timeFrom;
        String currentResultSetBucket = null;
        List<String> bufferedRow = null;
        while (current.isBefore(timeTo)) {
            String currentBucket = current.format(formatter);
            List<String> row = new ArrayList<>();
            row.add(currentBucket);
            if (currentResultSetBucket == null && rs.next()) {
                currentResultSetBucket = rs.getString("bucket");
                bufferedRow = createRowFromResultSet(rs, origin, brandId, categoryId, aggregates);
            }
            if (currentResultSetBucket != null && currentResultSetBucket.equals(currentBucket)) {
                row.addAll(bufferedRow);
                currentResultSetBucket = null;
                bufferedRow = null;
            } else {
                addEmptyData(row, action, origin, brandId, categoryId, aggregates);
            }
            rows.add(row);
            current = current.plusMinutes(1L);
        }
        result.setRows(rows);
        return result;
    }

    private List<String> createRowFromResultSet(ResultSet rs, String origin, String brandId, String categoryId, List<Aggregate> aggregates) throws SQLException {
        List<String> row = new ArrayList<>();
        row.add(rs.getString("action"));
        if (origin != null)
            row.add(rs.getString("origin"));
        if (brandId != null)
            row.add(rs.getString("brand_id"));
        if (categoryId != null)
            row.add(rs.getString("category_id"));
        for (Aggregate aggregate : aggregates) {
            switch (aggregate) {
                case COUNT:
                    row.add(String.valueOf(rs.getLong("count")));
                    break;
                case SUM_PRICE:
                    row.add(String.valueOf(rs.getLong("sum_price")));
                    break;
            }
        }
        return row;
    }

    private void addEmptyData(List<String> row, Action action, String origin, String brandId, String categoryId, List<Aggregate> aggregates) {
        row.add(action.name());
        if (origin != null)
            row.add(origin);
        if (brandId != null)
            row.add(brandId);
        if (categoryId != null)
            row.add(categoryId);
        for (Aggregate aggregate : aggregates)
            row.add("0");
    }
}