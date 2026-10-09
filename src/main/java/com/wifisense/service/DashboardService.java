package com.wifisense.service;

import com.wifisense.dto.AiPredictionResponse;
import com.wifisense.dto.AlertResponse;
import com.wifisense.dto.DashboardSummary;
import com.wifisense.model.Alert;
import com.wifisense.model.NetworkStatus;
import com.wifisense.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final NetworkRepository networks;
    private final DeviceRepository devices;
    private final AlertRepository alerts;
    private final AiPredictionRepository predictions;
    private final MeasurementRepository measurements;
    private final Clock clock;

    public DashboardService(NetworkRepository networks, DeviceRepository devices, AlertRepository alerts,
                            AiPredictionRepository predictions, MeasurementRepository measurements, Clock clock) {
        this.networks = networks;
        this.devices = devices;
        this.alerts = alerts;
        this.predictions = predictions;
        this.measurements = measurements;
        this.clock = clock;
    }

    public DashboardSummary summary() {
        Instant now = Instant.now(clock);
        Map<NetworkStatus, Long> byStatus = new EnumMap<>(NetworkStatus.class);
        Arrays.stream(NetworkStatus.values()).forEach(status -> byStatus.put(status, 0L));
        networks.countByStatus().forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));

        Double avgLatency = measurements.averageLatencySince(now.minus(Duration.ofHours(1)));
        return new DashboardSummary(
                byStatus.values().stream().mapToLong(Long::longValue).sum(),
                byStatus,
                devices.count(),
                alerts.countByStatusNot(Alert.Status.RESOLVED),
                predictions.countByAnomalyDetectedTrueAndCreatedAtAfter(now.minus(Duration.ofHours(24))),
                avgLatency == null ? null : Math.round(avgLatency * 100) / 100.0,
                alerts.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).stream().map(AlertResponse::from).toList(),
                predictions.findByAnomalyDetectedTrueOrderByCreatedAtDesc(PageRequest.of(0, 5)).stream()
                        .map(AiPredictionResponse::from).toList());
    }
}
