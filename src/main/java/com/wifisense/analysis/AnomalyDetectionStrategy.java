package com.wifisense.analysis;

import com.wifisense.model.Measurement;
import com.wifisense.model.NetworkStatus;
import com.wifisense.model.TrafficObservation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Delegates detection to the Isolation Forest model in WiFiSense-ai and maps its verdict to a status. */
@Component
public class AnomalyDetectionStrategy implements AnalysisStrategy {

    private final AiAnalysisClient aiClient;

    public AnomalyDetectionStrategy(AiAnalysisClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public AnalysisType type() {
        return AnalysisType.ANOMALY_DETECTION;
    }

    @Override
    public String description() {
        return "Machine learning (Isolation Forest) anomaly detection in the AI service";
    }

    @Override
    public AnalysisOutcome analyze(AnalysisContext context) {
        AiAnalysisResponse response = aiClient.detectAnomalies(toRequest(context));
        NetworkStatus status = switch (response.severity()) {
            case HIGH -> NetworkStatus.CRITICAL;
            case MEDIUM, LOW -> NetworkStatus.WARNING;
            case NONE -> NetworkStatus.NORMAL;
        };
        return new AnalysisOutcome(status, response.anomalyScore(), response.message(), response);
    }

    static AiAnalysisRequest toRequest(AnalysisContext context) {
        // Traffic is stored separately; it is matched to measurements by collection timestamp.
        Map<Instant, TrafficObservation> trafficByTime = context.traffic().stream()
                .collect(Collectors.toMap(TrafficObservation::getObservedAt, Function.identity(), (a, b) -> a));

        List<AiAnalysisRequest.Sample> samples = context.history().stream()
                .map(m -> toSample(m, trafficByTime.get(m.getMeasuredAt())))
                .toList();
        return new AiAnalysisRequest(context.networkId(), context.simulatedData(), samples);
    }

    private static AiAnalysisRequest.Sample toSample(Measurement m, TrafficObservation traffic) {
        return new AiAnalysisRequest.Sample(m.getLatencyMs(), m.getJitterMs(), m.getPacketLossPct(),
                m.getBandwidthMbps(), m.getSignalStrengthDbm(),
                traffic == null ? null : traffic.getTrafficVolumeMb(),
                m.getConnectedDevices(),
                traffic == null ? null : traffic.getPacketCount(),
                m.getMeasuredAt());
    }
}
