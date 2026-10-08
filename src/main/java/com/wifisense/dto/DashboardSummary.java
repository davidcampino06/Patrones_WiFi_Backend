package com.wifisense.dto;

import com.wifisense.model.NetworkStatus;

import java.util.List;
import java.util.Map;

public record DashboardSummary(long totalNetworks, Map<NetworkStatus, Long> networksByStatus, long totalDevices,
                               long openAlerts, long anomaliesLast24h, Double averageLatencyLastHourMs,
                               List<AlertResponse> recentAlerts, List<AiPredictionResponse> recentAnomalies) {
}
