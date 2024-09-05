package com.pu429640;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.pu429640.services.MySqlReader;
import com.pu429640.services.UserTagRedisStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pu429640.domain.Action;
import com.pu429640.domain.Aggregate;
import com.pu429640.domain.AggregatesQueryResult;
import com.pu429640.domain.UserProfileResult;
import com.pu429640.domain.UserTagEvent;
import com.pu429640.services.KafkaProducerService;

@RestController
public class EchoClient {

    private static final Logger log = LoggerFactory.getLogger(EchoClient.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final UserTagRedisStorage userTagStorage;
    private final KafkaProducerService kafkaProducerService;
    private final MySqlReader mySqlReader;

    @Autowired
    public EchoClient(UserTagRedisStorage userTagStorage, KafkaProducerService kafkaProducerService, MySqlReader mySqlReader) {
        this.userTagStorage = userTagStorage;
        this.kafkaProducerService = kafkaProducerService;
        this.mySqlReader = mySqlReader;
        log.info("Version: 12");
    }

    @PostMapping("/user_tags")
    public ResponseEntity<Void> addUserTag(@RequestBody(required = false) UserTagEvent userTag) {
        if (userTag != null) {
            userTagStorage.addUserTag(userTag);
            kafkaProducerService.sendUserTagEvent(userTag);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user_profiles/{cookie}")
    public ResponseEntity<UserProfileResult> getUserProfile(@PathVariable("cookie") String cookie,
            @RequestParam("time_range") String timeRangeStr,
            @RequestParam(defaultValue = "200") int limit,
            @RequestBody(required = false) UserProfileResult expectedResult) {

        UserProfileResult result = userTagStorage.getUserProfile(cookie, timeRangeStr, limit);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/aggregates")
    public ResponseEntity<AggregatesQueryResult> getAggregates(@RequestParam("time_range") String timeRangeStr,
            @RequestParam("action") Action action,
            @RequestParam("aggregates") List<Aggregate> aggregates,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "brand_id", required = false) String brandId,
            @RequestParam(value = "category_id", required = false) String categoryId,
            @RequestBody(required = false) AggregatesQueryResult expectedResult) {

        // Parse time range
        String[] timeRange = timeRangeStr.split("_");
        if (timeRange.length != 2) {
            return ResponseEntity.badRequest().build();
        }
        LocalDateTime timeFrom = LocalDateTime.parse(timeRange[0], FORMATTER);
        LocalDateTime timeTo = LocalDateTime.parse(timeRange[1], FORMATTER);

        // Query data using MySqlReader
        AggregatesQueryResult result = mySqlReader.getAggregates(timeFrom, timeTo, action, aggregates, origin, brandId, categoryId);
        if (expectedResult != result) {
            log.info("key: a: {}, agg: {}, o: {}, b: {}, c: {}", action, aggregates, origin, brandId, categoryId);
        }
        return ResponseEntity.ok(result);
    }
}
