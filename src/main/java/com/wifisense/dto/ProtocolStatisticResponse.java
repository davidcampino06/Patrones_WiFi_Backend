package com.wifisense.dto;

import com.wifisense.model.ProtocolStatistic;

import java.time.Instant;

public record ProtocolStatisticResponse(String protocol, long packetCount, Instant periodStart, Instant periodEnd) {

    public static ProtocolStatisticResponse from(ProtocolStatistic p) {
        return new ProtocolStatisticResponse(p.getProtocol(), p.getPacketCount(), p.getPeriodStart(), p.getPeriodEnd());
    }
}
