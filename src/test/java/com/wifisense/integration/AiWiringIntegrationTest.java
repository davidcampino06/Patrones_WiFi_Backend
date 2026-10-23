package com.wifisense.integration;

import com.wifisense.analysis.AiAnalysisClient;
import com.wifisense.analysis.CircuitBreakerAiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** In the running application every service that asks the AI gets the Proxy with the Circuit Breaker, not the bare
 * HTTP adapter: that is what makes a down AI service answer at once instead of after the timeout. */
@EnabledIfEnvironmentVariable(named = PostgresIntegrationTest.DATABASE_URL, matches = ".+")
class AiWiringIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private AiAnalysisClient ai;

    @Test
    void theAiClientOfTheApplicationIsTheCircuitBreakerProxy() {
        assertThat(ai).isInstanceOf(CircuitBreakerAiClient.class);
        assertThat(((CircuitBreakerAiClient) ai).state()).isEqualTo(CircuitBreakerAiClient.State.CLOSED);
    }
}
