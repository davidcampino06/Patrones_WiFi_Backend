package com.wifisense.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CircuitBreakerAiClientTest {

    private static final AiQualityRequest QUALITY = new AiQualityRequest(20.0, 2.0, 0.0, null, null, null, -55.0);

    /** A clock the test moves forward. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-23T15:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final AiAnalysisClient target = mock(AiAnalysisClient.class);
    private final MovableClock clock = new MovableClock();
    private final CircuitBreakerAiClient breaker = new CircuitBreakerAiClient(target, 2, Duration.ofSeconds(30), clock);
    private final JsonNode ok = new ObjectMapper().createObjectNode().put("score", 90);

    private static AiServiceUnavailableException unreachable() {
        return new AiServiceUnavailableException("Falló la solicitud al servicio de IA: timeout",
                new ResourceAccessException("I/O error: Read timed out"));
    }

    @Test
    void whileTheServiceAnswersEveryCallGoesThrough() {
        when(target.qualityScore(any())).thenReturn(ok);

        assertThat(breaker.qualityScore(QUALITY)).isSameAs(ok);
        assertThat(breaker.qualityScore(QUALITY)).isSameAs(ok);

        verify(target, times(2)).qualityScore(QUALITY);
        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.CLOSED);
    }

    @Test
    void afterTwoFailuresInARowItAnswersAtOnceWithoutWaitingForTheService() {
        when(target.qualityScore(any())).thenThrow(unreachable());

        assertThatThrownBy(() -> breaker.qualityScore(QUALITY)).isInstanceOf(AiServiceUnavailableException.class);
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY)).isInstanceOf(AiServiceUnavailableException.class);
        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.OPEN);

        // the next calls of any kind fail at once: the service is not asked again (no 10 s wait)
        assertThatThrownBy(() -> breaker.securityAssess(null))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("se volverá a probar en 30 s");
        clock.advance(Duration.ofSeconds(20));
        assertThatThrownBy(() -> breaker.detectAnomalies(null)).hasMessageContaining("10 s");
        verify(target, times(2)).qualityScore(any());
        verify(target, never()).securityAssess(any());
        verify(target, never()).detectAnomalies(any());
    }

    @Test
    void aSuccessBetweenFailuresStartsTheCountAgain() {
        when(target.qualityScore(any())).thenThrow(unreachable()).thenReturn(ok).thenThrow(unreachable());

        assertThatThrownBy(() -> breaker.qualityScore(QUALITY)).isInstanceOf(AiServiceUnavailableException.class);
        breaker.qualityScore(QUALITY);
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY)).isInstanceOf(AiServiceUnavailableException.class);

        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.CLOSED);
    }

    @Test
    void afterTheWaitOneTestCallClosesTheCircuitWhenTheServiceIsBack() {
        when(target.qualityScore(any())).thenThrow(unreachable()).thenThrow(unreachable()).thenReturn(ok);
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));

        clock.advance(Duration.ofSeconds(30));
        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.HALF_OPEN);
        assertThat(breaker.qualityScore(QUALITY)).isSameAs(ok);

        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.CLOSED);
        verify(target, times(3)).qualityScore(any());
    }

    @Test
    void aFailedTestCallOpensTheCircuitForAnotherPeriod() {
        when(target.qualityScore(any())).thenThrow(unreachable());
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        clock.advance(Duration.ofSeconds(31));

        assertThatThrownBy(() -> breaker.qualityScore(QUALITY)).isInstanceOf(AiServiceUnavailableException.class);

        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.OPEN);
        verify(target, times(3)).qualityScore(any());
    }

    @Test
    void whileTheTestCallIsRunningOtherCallsDoNotReachTheService() {
        when(target.qualityScore(any())).thenThrow(unreachable()).thenThrow(unreachable());
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        clock.advance(Duration.ofSeconds(30));
        AtomicReference<Throwable> concurrent = new AtomicReference<>();
        doAnswer(invocation -> {
            // another report arrives while the test call is still waiting for the service
            try {
                breaker.securityAssess(null);
            } catch (RuntimeException e) {
                concurrent.set(e);
            }
            return ok;
        }).when(target).channelRecommend(any());

        assertThat(breaker.channelRecommend(null)).isSameAs(ok);

        assertThat(concurrent.get()).isInstanceOf(AiServiceUnavailableException.class);
        verify(target, never()).securityAssess(any());
        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.CLOSED);
    }

    @Test
    void aRefusedRequestMeansTheServiceIsUpAndDoesNotOpenTheCircuit() {
        AiServiceUnavailableException refused = new AiServiceUnavailableException("Falló el entrenamiento",
                HttpClientErrorException.create(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", null, null, null));
        when(target.trainModel(any())).thenThrow(refused);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> breaker.trainModel(null)).isSameAs(refused);
        }

        assertThat(breaker.state()).isEqualTo(CircuitBreakerAiClient.State.CLOSED);
        verify(target, times(5)).trainModel(any());
    }

    @Test
    void theHealthCheckAlwaysAsksTheService() {
        when(target.qualityScore(any())).thenThrow(unreachable());
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        assertThatThrownBy(() -> breaker.qualityScore(QUALITY));
        when(target.isHealthy()).thenReturn(false);

        assertThat(breaker.isHealthy()).isFalse();
        verify(target).isHealthy();
    }

    @Test
    void rejectsAnImpossibleConfiguration() {
        assertThatThrownBy(() -> new CircuitBreakerAiClient(target, 0, Duration.ofSeconds(30), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CircuitBreakerAiClient(target, 2, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
