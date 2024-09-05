package com.pu429640.config;

public class Config {
    public static String get(String key) {
        return System.getenv(key);
    }
}
