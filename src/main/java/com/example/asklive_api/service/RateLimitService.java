package com.example.asklive_api.service;

import com.example.asklive_api.error.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MILLIS = 10_000L;
    private static final int MAX_TRACKED_TOKENS = 10_000;

    private final Map<String, Deque<Long>> requestTimes = new ConcurrentHashMap<>();

    public void check(String participantToken) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestTimes.computeIfAbsent(participantToken, key -> new ArrayDeque<>());
        boolean allowed;
        synchronized (timestamps) {
            evictExpired(timestamps, now);
            allowed = timestamps.size() < MAX_REQUESTS;
            if (allowed) {
                timestamps.addLast(now);
            }
        }
        if (requestTimes.size() > MAX_TRACKED_TOKENS) {
            forgetExpiredTokens(now);
        }
        if (!allowed) {
            throw ApiException.tooManyRequests("Too many requests, please slow down");
        }
    }

    private void forgetExpiredTokens(long now) {
        requestTimes.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                evictExpired(timestamps, now);
                return timestamps.isEmpty();
            }
        });
    }

    private void evictExpired(Deque<Long> timestamps, long now) {
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MILLIS) {
            timestamps.pollFirst();
        }
    }

}
