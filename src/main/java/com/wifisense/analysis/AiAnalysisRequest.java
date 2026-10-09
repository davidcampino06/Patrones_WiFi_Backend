package com.wifisense.analysis;

import java.time.Instant;
import java.util.List;

/** Contract of POST /api/v1/anomalies/detect in WiFiSense-ai. */
public record AiAnalysisRequest(long networkId, boolean simulatedData, List<Sample> measurements) {

    public record Sample(double latency, double jitter, double packetLoss, Double bandwidth, Integer signalStrength,
                         Double trafficVolume, Integer connectedDevices, Long packetCount, Instant timestamp) {
    }
}
