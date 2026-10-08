package com.wifisense.dto;

import com.wifisense.model.AiPrediction;

import java.time.Instant;

public record AiPredictionResponse(Long id, Long networkId, String ssid, Long analysisResultId,
                                   boolean anomalyDetected, double anomalyScore, AiPrediction.Severity severity,
                                   String message, String recommendation, String modelVersion,
                                   boolean simulatedData, Instant createdAt) {

    public static AiPredictionResponse from(AiPrediction p) {
        return new AiPredictionResponse(p.getId(), p.getNetwork().getId(), p.getNetwork().getSsid(),
                p.getAnalysisResult().getId(), p.isAnomalyDetected(), p.getAnomalyScore(), p.getSeverity(),
                p.getMessage(), p.getRecommendation(), p.getModelVersion(), p.isSimulatedData(), p.getCreatedAt());
    }
}
