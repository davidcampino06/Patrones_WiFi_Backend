package com.wifisense.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * An access point (BSSID) the agent of a network has seen around it. Stored once: each scan that sees it again only
 * updates when it was last seen, how many times and its current name. The BSSID itself never reaches the server:
 * only its fingerprint with the agent's salt and its last byte, to recognize one's own router on its sticker.
 */
@Entity
@Table(name = "wifi_access_points")
public class WifiAccessPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_network_id", nullable = false, updatable = false)
    private HomeNetwork network;

    @Column(name = "bssid_fingerprint", nullable = false, length = 24, updatable = false, columnDefinition = "CHAR(24)")
    private String bssidFingerprint;

    @Column(name = "bssid_suffix", length = 2, columnDefinition = "CHAR(2)")
    private String bssidSuffix;

    @Column(length = 60)
    private String vendor;

    /** Null for a hidden network. */
    @Column(length = 64)
    private String ssid;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "times_seen", nullable = false)
    private int timesSeen;

    protected WifiAccessPoint() {
    }

    public WifiAccessPoint(HomeNetwork network, String bssidFingerprint, Instant now) {
        this.network = network;
        this.bssidFingerprint = bssidFingerprint;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
        this.hidden = true;
    }

    /**
     * A scan saw it (the first one too): its name can change (or hide), and the maker and the suffix fill in when
     * they were missing.
     */
    public void seen(String ssid, String vendor, String bssidSuffix, Instant now) {
        this.ssid = ssid;
        this.hidden = ssid == null;
        if (vendor != null) {
            this.vendor = vendor;
        }
        if (bssidSuffix != null) {
            this.bssidSuffix = bssidSuffix;
        }
        if (now.isAfter(lastSeenAt)) {
            this.lastSeenAt = now;
        }
        this.timesSeen = timesSeen + 1;
    }

    public Long getId() { return id; }
    public HomeNetwork getNetwork() { return network; }
    public String getBssidFingerprint() { return bssidFingerprint; }
    public String getBssidSuffix() { return bssidSuffix; }
    public String getVendor() { return vendor; }
    public String getSsid() { return ssid; }
    public boolean isHidden() { return hidden; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public int getTimesSeen() { return timesSeen; }
}
