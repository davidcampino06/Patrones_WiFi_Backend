package com.wifisense.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.wifisense.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private static final class MovableClock extends Clock {
        private Instant now = NOW;

        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    private final MovableClock clock = new MovableClock();
    private final LoginAttemptService attempts = new LoginAttemptService(clock);

    @Test
    void blocksUserAfterRepeatedFailures() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_USER; i++) {
            attempts.recordFailure("analyst", "1.2.3.4");
        }

        assertThat(attempts.isBlocked("analyst", "1.2.3.4")).isTrue();
        assertThat(attempts.isBlocked("analyst", "5.6.7.8")).isFalse();
    }

    @Test
    void blockExpiresAfterTheWindow() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_USER; i++) {
            attempts.recordFailure("analyst", "1.2.3.4");
        }
        clock.now = NOW.plus(LoginAttemptService.WINDOW).plus(Duration.ofSeconds(1));

        assertThat(attempts.isBlocked("analyst", "1.2.3.4")).isFalse();
    }

    @Test
    void successfulLoginClearsUserFailures() {
        attempts.recordFailure("analyst", "1.2.3.4");
        attempts.recordSuccess("analyst", "1.2.3.4");

        assertThat(attempts.isBlocked("analyst", "1.2.3.4")).isFalse();
    }

    @Test
    void blocksAddressThatTriesManyUsernames() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ADDRESS; i++) {
            attempts.recordFailure("user" + i, "9.9.9.9");
        }

        assertThat(attempts.isBlocked("someone-else", "9.9.9.9")).isTrue();
    }
}
