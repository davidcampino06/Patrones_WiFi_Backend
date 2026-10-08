package com.wifisense.dto;

import com.wifisense.model.TrafficSession;

import java.time.Instant;

public record TrafficSessionResponse(Long id, String protocol, Integer destinationPort, long bytesSent,
                                     long bytesReceived, Instant startedAt, Instant endedAt) {

    public static TrafficSessionResponse from(TrafficSession s) {
        return new TrafficSessionResponse(s.getId(), s.getProtocol(), s.getDestinationPort(), s.getBytesSent(),
                s.getBytesReceived(), s.getStartedAt(), s.getEndedAt());
    }
}
