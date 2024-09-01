package com.pu429640;

import java.util.List;

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
import com.pu429640.services.UserTagStorage;

@RestController
public class EchoClient {

    private static final Logger log = LoggerFactory.getLogger(EchoClient.class);

    private final UserTagStorage userTagStorage;

    @Autowired
    public EchoClient(UserTagStorage userTagStorage) {
        this.userTagStorage = userTagStorage;
    }

    @PostMapping("/user_tags")
    public ResponseEntity<Void> addUserTag(@RequestBody(required = false) UserTagEvent userTag) {
        if (userTag != null) {
            userTagStorage.addUserTag(userTag);
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

        return ResponseEntity.ok(expectedResult);
    }
}
