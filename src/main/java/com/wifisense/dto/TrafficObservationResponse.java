package com.wifisense.dto;

import com.wifisense.model.TrafficObservation;

import java.time.Instant;

public record TrafficObservationResponse(Long id, double trafficVolumeMb, long packetCount, Instant observedAt) {

    public static TrafficObservationResponse from(TrafficObservation t) {
        return new TrafficObservationResponse(t.getId(), t.getTrafficVolumeMb(), t.getPacketCount(), t.getObservedAt());
    }
}
