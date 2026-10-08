package com.wifisense.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "protocol_statistics")
public class ProtocolStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @Column(nullable = false, length = 20)
    private String protocol;

    @Column(name = "packet_count", nullable = false)
    private long packetCount;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    protected ProtocolStatistic() {
    }

    public ProtocolStatistic(Network network, String protocol, long packetCount, Instant periodStart, Instant periodEnd) {
        this.network = network;
        this.protocol = protocol;
        this.packetCount = packetCount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public String getProtocol() { return protocol; }
    public long getPacketCount() { return packetCount; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
}
