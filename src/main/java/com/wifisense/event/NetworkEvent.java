package com.wifisense.event;

import java.time.Instant;

public sealed interface NetworkEvent
        permits MeasurementCollectedEvent, AnalysisCompletedEvent, NetworkStatusChangedEvent {

    long networkId();

    String ssid();

    Instant occurredAt();

    String describe();
}
