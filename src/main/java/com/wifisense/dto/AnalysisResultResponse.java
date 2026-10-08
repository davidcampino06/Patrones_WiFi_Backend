package com.wifisense.dto;

import com.wifisense.analysis.AnalysisType;
import com.wifisense.model.AiPrediction;
import com.wifisense.model.AnalysisResult;
import com.wifisense.model.NetworkStatus;

import java.time.Instant;

public record AnalysisResultResponse(Long id, Long networkId, String ssid, AnalysisType type,
                                     NetworkStatus detectedStatus, NetworkStatus networkStatus, double score,
                                     String summary, int measurementCount, String requestedBy, Instant createdAt,
                                     AiPredictionResponse prediction) {

    public static AnalysisResultResponse from(AnalysisResult r, AiPrediction prediction) {
        return new AnalysisResultResponse(r.getId(), r.getNetwork().getId(), r.getNetwork().getSsid(), r.getType(),
                r.getDetectedStatus(), r.getNetwork().getStatus(), r.getScore(), r.getSummary(),
                r.getMeasurementCount(), r.getRequestedBy() == null ? "system" : r.getRequestedBy().getUsername(),
                r.getCreatedAt(), prediction == null ? null : AiPredictionResponse.from(prediction));
    }
}
