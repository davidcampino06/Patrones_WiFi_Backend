package com.wifisense.model;

import com.wifisense.network.DataSourceType;
import com.wifisense.network.NetworkSnapshot;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "measurements")
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @Column(name = "latency_ms", nullable = false)
    private double latencyMs;

    @Column(name = "jitter_ms", nullable = false)
    private double jitterMs;

    @Column(name = "packet_loss_pct", nullable = false)
    private double packetLossPct;

    @Column(name = "bandwidth_mbps")
    private Double bandwidthMbps;

    @Column(name = "signal_strength_dbm")
    private Integer signalStrengthDbm;

    @Column(name = "connected_devices")
    private Integer connectedDevices;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DataSourceType source;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Measurement() {
    }

    public static Measurement from(Network network, NetworkSnapshot snapshot, DataSourceType source) {
        Measurement measurement = new Measurement();
        measurement.network = network;
        measurement.latencyMs = snapshot.latencyMs();
        measurement.jitterMs = snapshot.jitterMs();
        measurement.packetLossPct = snapshot.packetLossPct();
        measurement.bandwidthMbps = snapshot.bandwidthMbps();
        measurement.signalStrengthDbm = snapshot.signalStrengthDbm();
        measurement.connectedDevices = snapshot.connectedDevices();
        measurement.source = source;
        measurement.measuredAt = snapshot.collectedAt();
        return measurement;
    }

    public boolean isSimulated() {
        return source == DataSourceType.SIMULATION;
    }

    public Long getId() { return id; }
    public Network getNetwork() { return network; }
    public double getLatencyMs() { return latencyMs; }
    public double getJitterMs() { return jitterMs; }
    public double getPacketLossPct() { return packetLossPct; }
    public Double getBandwidthMbps() { return bandwidthMbps; }
    public Integer getSignalStrengthDbm() { return signalStrengthDbm; }
    public Integer getConnectedDevices() { return connectedDevices; }
    public DataSourceType getSource() { return source; }
    public Instant getMeasuredAt() { return measuredAt; }
}
