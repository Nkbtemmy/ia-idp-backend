package com.urutare.sso.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LinkedInRateLimiter {
    private final Map<String, List<Long>> requestTimes = new ConcurrentHashMap<>();

    public boolean isAllowed(String clientIp) {
        // Implement rate limiting logic
        return true;
    }
}
