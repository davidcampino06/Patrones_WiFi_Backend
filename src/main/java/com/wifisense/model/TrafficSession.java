package com.wifisense.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "traffic_sessions")
public class TrafficSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 20)
    private String protocol;

    @Column(name = "destination_port")
    private Integer destinationPort;

    @Column(name = "bytes_sent", nullable = false)
    private long bytesSent;

    @Column(name = "bytes_received", nullable = false)
    private long bytesReceived;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected TrafficSession() {
    }

    public Long getId() { return id; }
    public String getProtocol() { return protocol; }
    public Integer getDestinationPort() { return destinationPort; }
    public long getBytesSent() { return bytesSent; }
    public long getBytesReceived() { return bytesReceived; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
}
