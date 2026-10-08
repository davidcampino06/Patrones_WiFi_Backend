package com.wifisense.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    public enum Type { LAPTOP, PHONE, TABLET, IOT, ACCESS_POINT, UNKNOWN }

    public enum ConnectionStatus { CONNECTED, IDLE, DISCONNECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @Column(length = 100)
    private String hostname;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 20)
    private ConnectionStatus connectionStatus = ConnectionStatus.CONNECTED;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Device() {
    }

    public Device(Network network, String hostname, String ipAddress, String macAddress, Type type,
                  Integer signalStrength) {
        this.network = network;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress.toUpperCase();
        this.type = type;
        this.signalStrength = signalStrength;
    }

    public Long getId() { return id; }
    public Network getNetwork() { return network; }
    public String getHostname() { return hostname; }
    public String getIpAddress() { return ipAddress; }
    public String getMacAddress() { return macAddress; }
    public Type getType() { return type; }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public Integer getSignalStrength() { return signalStrength; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
