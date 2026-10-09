package com.wifisense.analysis;

import com.wifisense.model.NetworkStatus;

import java.util.Optional;

/** Result of a strategy. aiResponse is present only when the AI service took part. */
public record AnalysisOutcome(NetworkStatus status, double score, String summary, AiAnalysisResponse aiResponse) {

    public AnalysisOutcome {
        score = Math.max(0, Math.min(1, score));
    }

    public static AnalysisOutcome of(NetworkStatus status, double score, String summary) {
        return new AnalysisOutcome(status, score, summary, null);
    }

    public Optional<AiAnalysisResponse> ai() {
        return Optional.ofNullable(aiResponse);
    }
}
