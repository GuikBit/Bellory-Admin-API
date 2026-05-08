package org.exemplo.belloryadmin.service.tracking;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TrackingRateLimiterService {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientIp) {
        AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }

    @Scheduled(fixedRate = 60000)
    public void resetCounts() {
        requestCounts.clear();
    }
}
