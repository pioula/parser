package com.pu429640.services;

import com.pu429640.domain.UserProfileResult;
import com.pu429640.domain.UserTagEvent;

public interface IUserTagStorage {
    void addUserTag(UserTagEvent event);
    UserProfileResult getUserProfile(String cookie, String timeRangeStr, int limit);
}