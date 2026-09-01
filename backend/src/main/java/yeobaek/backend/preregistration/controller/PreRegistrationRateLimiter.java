package yeobaek.backend.preregistration.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.TooManyRequestsException;

class PreRegistrationRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupAt = new AtomicLong(Long.MIN_VALUE);

    void check(String clientIp) {
        check(clientIp, Instant.now());
    }

    void check(String clientIp, Instant now) {
        removeExpiredWindows(now);

        AtomicBoolean exceeded = new AtomicBoolean(false);
        windows.compute(clientIp, (ip, window) -> {
            if (window == null || window.isExpiredAt(now)) {
                return new RequestWindow(now, 1);
            }
            if (window.requestCount() >= MAX_REQUESTS) {
                exceeded.set(true);
                return window;
            }
            return window.increment();
        });

        if (exceeded.get()) {
            throw new TooManyRequestsException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    int trackedClientCount() {
        return windows.size();
    }

    private void removeExpiredWindows(Instant now) {
        long nowEpochMilli = now.toEpochMilli();
        long scheduledCleanup = nextCleanupAt.get();
        if (nowEpochMilli < scheduledCleanup
                || !nextCleanupAt.compareAndSet(scheduledCleanup, now.plus(WINDOW_DURATION).toEpochMilli())) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().isExpiredAt(now));
    }

    private record RequestWindow(Instant startedAt, int requestCount) {

        private boolean isExpiredAt(Instant instant) {
            return !instant.isBefore(startedAt.plus(WINDOW_DURATION));
        }

        private RequestWindow increment() {
            return new RequestWindow(startedAt, requestCount + 1);
        }
    }
}
