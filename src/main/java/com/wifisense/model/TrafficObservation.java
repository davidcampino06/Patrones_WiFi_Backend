package com.wifisense.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "traffic_observations")
public class TrafficObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @Column(name = "traffic_volume_mb", nullable = false)
    private double trafficVolumeMb;

    @Column(name = "packet_count", nullable = false)
    private long packetCount;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TrafficObservation() {
    }

    public TrafficObservation(Network network, double trafficVolumeMb, long packetCount, Instant observedAt) {
        this.network = network;
        this.trafficVolumeMb = trafficVolumeMb;
        this.packetCount = packetCount;
        this.observedAt = observedAt;
    }

    public Long getId() { return id; }
    public double getTrafficVolumeMb() { return trafficVolumeMb; }
    public long getPacketCount() { return packetCount; }
    public Instant getObservedAt() { return observedAt; }
}
