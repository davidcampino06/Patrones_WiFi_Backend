package com.wifisense.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: too many failed logins for a user from one address, or from one address overall,
 * blocks further attempts for a while. In-memory is enough for a single backend instance.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES_PER_USER = 5;
    static final int MAX_FAILURES_PER_ADDRESS = 20;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private record Failures(int count, Instant firstAt) {
    }

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String username, String address) {
        return reached(userKey(username, address), MAX_FAILURES_PER_USER)
                || reached(addressKey(address), MAX_FAILURES_PER_ADDRESS);
    }

    public void recordFailure(String username, String address) {
        Instant now = Instant.now(clock);
        for (String key : List.of(userKey(username, address), addressKey(address))) {
            failures.compute(key, (k, current) -> current == null || expired(current, now)
                    ? new Failures(1, now)
                    : new Failures(current.count() + 1, current.firstAt()));
        }
    }

    public void recordSuccess(String username, String address) {
        failures.remove(userKey(username, address));
    }

    private boolean reached(String key, int limit) {
        Failures current = failures.get(key);
        if (current == null) {
            return false;
        }
        if (expired(current, Instant.now(clock))) {
            failures.remove(key);
            return false;
        }
        return current.count() >= limit;
    }

    private static boolean expired(Failures failures, Instant now) {
        return failures.firstAt().plus(WINDOW).isBefore(now);
    }

    private static String userKey(String username, String address) {
        return "user:" + (username == null ? "" : username.toLowerCase()) + "|" + address;
    }

    private static String addressKey(String address) {
        return "address:" + address;
    }
}
