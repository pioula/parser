package com.pu429640.services;

import com.pu429640.domain.UserProfileResult;
import com.pu429640.domain.UserTagEvent;
import com.pu429640.domain.Action;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserTagLocalStorage implements IUserTagStorage {

    private static final int MAX_TAGS_PER_TYPE = 200;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final Map<String, Map<Action, List<UserTagEvent>>> storage = new ConcurrentHashMap<>();

    @Override
    public synchronized void addUserTag(UserTagEvent event) {
        if (event == null || event.getCookie() == null || event.getAction() == null) {
            return; // Ignore null events or events with null cookie or action
        }

        String cookie = event.getCookie();
        Action action = event.getAction();

        storage.computeIfAbsent(cookie, k -> new ConcurrentHashMap<>())
               .computeIfAbsent(action, k -> new ArrayList<>())
               .add(event);

        List<UserTagEvent> tags = storage.get(cookie).get(action);
        if (tags.size() > MAX_TAGS_PER_TYPE) {
            synchronized (tags) {
                tags.sort(Comparator.comparing(UserTagEvent::getTime).reversed());
                storage.get(cookie).put(action, new ArrayList<>(tags.subList(0, MAX_TAGS_PER_TYPE)));
            }
        }
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

        Map<Action, List<UserTagEvent>> cookieTags = storage.getOrDefault(cookie, new ConcurrentHashMap<>());

        List<UserTagEvent> views = filterAndLimitTags(cookieTags.getOrDefault(Action.VIEW, new ArrayList<>()), start, end, limit);
        List<UserTagEvent> buys = filterAndLimitTags(cookieTags.getOrDefault(Action.BUY, new ArrayList<>()), start, end, limit);

        return new UserProfileResult(cookie, views, buys);
    }

    private List<UserTagEvent> filterAndLimitTags(List<UserTagEvent> tags, Instant start, Instant end, int limit) {
        return tags.stream()
                   .filter(tag -> tag.getTime() != null && !tag.getTime().isBefore(start) && tag.getTime().isBefore(end))
                   .sorted(Comparator.comparing(UserTagEvent::getTime).reversed())
                   .limit(limit)
                   .collect(Collectors.toList());
    }
}