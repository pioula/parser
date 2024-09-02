package com.pu429640.services;

import com.pu429640.domain.UserProfileResult;
import com.pu429640.domain.UserTagEvent;
import com.pu429640.EchoClient;
import com.pu429640.domain.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserTagRedisStorage implements IUserTagStorage {
    private static final Logger log = LoggerFactory.getLogger(UserTagRedisStorage.class);


    private static final int MAX_TAGS_PER_TYPE = 200;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final String KEY_PREFIX = "userTag:";

    @Autowired
    private final RedisTemplate<String, UserTagEvent> redisTemplate;
    
    @Autowired
    public UserTagRedisStorage(RedisTemplate<String, UserTagEvent> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addUserTag(UserTagEvent event) {
        if (event == null || event.getCookie() == null || event.getAction() == null) {
            return; // Ignore null events or events with null cookie or action
        }

        String key = getKey(event.getCookie(), event.getAction());
        redisTemplate.opsForZSet().add(key, event, event.getTime().toEpochMilli());

        // Trim the set to keep only the most recent MAX_TAGS_PER_TYPE events
        redisTemplate.opsForZSet().removeRange(key, 0, -MAX_TAGS_PER_TYPE - 1);
    }

    @Override
    public UserProfileResult getUserProfile(String cookie, String timeRangeStr, int limit) {
        if (cookie == null || timeRangeStr == null) {
            return new UserProfileResult(cookie, new ArrayList<>(), new ArrayList<>());
        }

        String[] range = timeRangeStr.split("_");
        if (range.length != 2) {
            return new UserProfileResult(cookie, new ArrayList<>(), new ArrayList<>());
        }

        Instant start, end;
        try {
            start = LocalDateTime.parse(range[0], DATE_TIME_FORMATTER).toInstant(ZoneOffset.UTC);
            end = LocalDateTime.parse(range[1], DATE_TIME_FORMATTER).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return new UserProfileResult(cookie, new ArrayList<>(), new ArrayList<>());
        }

        List<UserTagEvent> views = getFilteredEvents(cookie, Action.VIEW, start, end, limit);
        List<UserTagEvent> buys = getFilteredEvents(cookie, Action.BUY, start, end, limit);

        return new UserProfileResult(cookie, views, buys);
    }

    private List<UserTagEvent> getFilteredEvents(String cookie, Action action, Instant start, Instant end, int limit) {
        String key = getKey(cookie, action);
        Set<UserTagEvent> events = redisTemplate.opsForZSet().reverseRangeByScore(
                key,
                start.toEpochMilli(),
                end.toEpochMilli() - 1,
                0,
                limit
        );

        if (events == null) {
            return new ArrayList<>();
        }

        return events.stream()
                .filter(tag -> tag.getTime() != null)
                .sorted((a, b) -> b.getTime().compareTo(a.getTime()))
                .collect(Collectors.toList());
    }

    private String getKey(String cookie, Action action) {
        return KEY_PREFIX + cookie + ":" + action.name();
    }
}