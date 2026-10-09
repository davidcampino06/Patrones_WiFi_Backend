package com.wifisense.event;

import com.wifisense.analysis.AnalysisType;
import com.wifisense.model.NetworkStatus;

import java.time.Instant;

public record AnalysisCompletedEvent(long networkId, String ssid, long analysisResultId, AnalysisType type,
                                     NetworkStatus detectedStatus, double score, Instant occurredAt)
        implements NetworkEvent {

    @Override
    public String describe() {
        return "%s analysis detected %s (score %.2f)".formatted(type, detectedStatus, score);
    }
}
