package com.wifisense.dto;

import com.wifisense.model.Alert;

import java.time.Instant;

public record AlertResponse(Long id, Long networkId, String ssid, Long analysisResultId, Alert.Severity severity,
                            String message, Alert.Status status, Instant createdAt, Instant resolvedAt) {

    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getNetwork().getId(), a.getNetwork().getSsid(),
                a.getAnalysisResult() == null ? null : a.getAnalysisResult().getId(), a.getSeverity(),
                a.getMessage(), a.getStatus(), a.getCreatedAt(), a.getResolvedAt());
    }
}
