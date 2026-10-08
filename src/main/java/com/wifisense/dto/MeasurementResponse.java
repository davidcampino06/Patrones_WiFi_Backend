package com.wifisense.dto;

import com.wifisense.model.Measurement;
import com.wifisense.network.DataSourceType;

import java.time.Instant;

public record MeasurementResponse(Long id, Long networkId, double latencyMs, double jitterMs, double packetLossPct,
                                  Double bandwidthMbps, Integer signalStrengthDbm, Integer connectedDevices,
                                  DataSourceType source, boolean simulated, Instant measuredAt) {

    public static MeasurementResponse from(Measurement m) {
        return new MeasurementResponse(m.getId(), m.getNetwork().getId(), m.getLatencyMs(), m.getJitterMs(),
                m.getPacketLossPct(), m.getBandwidthMbps(), m.getSignalStrengthDbm(), m.getConnectedDevices(),
                m.getSource(), m.isSimulated(), m.getMeasuredAt());
    }
}
