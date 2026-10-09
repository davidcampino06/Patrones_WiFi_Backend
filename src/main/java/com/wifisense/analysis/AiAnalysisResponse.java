package com.wifisense.analysis;

import com.wifisense.model.AiPrediction;

import java.util.List;

public record AiAnalysisResponse(
        boolean anomalyDetected,
        double anomalyScore,
        AiPrediction.Severity severity,
        String message,
        String recommendation,
        List<String> contributingFeatures,
        int anomalousSamples,
        int sampleSize,
        List<String> imputedFeatures,
        String modelVersion,
        boolean simulatedData) {
}
