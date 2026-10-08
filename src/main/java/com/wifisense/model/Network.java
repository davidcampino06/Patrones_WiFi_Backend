package com.wifisense.model;

import com.wifisense.network.DataSourceType;
import com.wifisense.network.state.NetworkState;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "networks")
public class Network {

    public enum SecurityType { OPEN, WPA2, WPA3, WPA2_ENTERPRISE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, length = 64)
    private String ssid;

    @Column(nullable = false, unique = true, length = 17)
    private String bssid;

    @Column(name = "frequency_band", nullable = false, length = 10)
    private String frequencyBand;

    @Column(nullable = false)
    private int channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "security_type", nullable = false, length = 20)
    private SecurityType securityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NetworkStatus status = NetworkStatus.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source_type", nullable = false, length = 30)
    private DataSourceType dataSourceType;

    @Column(name = "last_collected_at")
    private Instant lastCollectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Network() {
    }

    public Network(Zone zone, String ssid, String bssid, String frequencyBand, int channel,
                   SecurityType securityType, DataSourceType dataSourceType) {
        this.zone = zone;
        this.dataSourceType = dataSourceType;
        update(ssid, bssid, frequencyBand, channel, securityType);
    }

    public void update(String ssid, String bssid, String frequencyBand, int channel, SecurityType securityType) {
        this.ssid = ssid;
        this.bssid = bssid.toUpperCase();
        this.frequencyBand = frequencyBand;
        this.channel = channel;
        this.securityType = securityType;
    }

    public void moveTo(Zone newZone, DataSourceType newDataSource) {
        this.zone = newZone;
        this.dataSourceType = newDataSource;
    }

    /** Behaviour that depends on the current status is delegated to its State object. */
    public NetworkState state() {
        return NetworkState.of(status);
    }

    /** Lets the current state decide the next status; returns true when the status changed. */
    public boolean applyDetectedStatus(NetworkStatus detected) {
        NetworkStatus next = state().next(detected);
        boolean changed = next != status;
        status = next;
        return changed;
    }

    public boolean isCollectionDue(Instant now) {
        return lastCollectedAt == null || !lastCollectedAt.plus(state().collectionInterval()).isAfter(now);
    }

    public void markCollected(Instant collectedAt) {
        this.lastCollectedAt = collectedAt;
    }

    public Long getId() { return id; }
    public Zone getZone() { return zone; }
    public String getSsid() { return ssid; }
    public String getBssid() { return bssid; }
    public String getFrequencyBand() { return frequencyBand; }
    public int getChannel() { return channel; }
    public SecurityType getSecurityType() { return securityType; }
    public NetworkStatus getStatus() { return status; }
    public DataSourceType getDataSourceType() { return dataSourceType; }
    public Instant getLastCollectedAt() { return lastCollectedAt; }
}
