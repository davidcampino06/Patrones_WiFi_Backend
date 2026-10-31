package com.wifisense.model;

import jakarta.persistence.*;

/**
 * What one scan saw of one access point. Every field the system did not report is null: nothing is estimated here
 * (the signal in dBm, for example, is stored only when the system gave it).
 */
@Entity
@Table(name = "wifi_scan_observations")
public class WifiScanObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, updatable = false)
    private WifiScanSession scan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "access_point_id", nullable = false, updatable = false)
    private WifiAccessPoint accessPoint;

    @Column(length = 64)
    private String ssid;

    @Column(name = "signal_percent")
    private Integer signalPercent;

    @Column(name = "signal_dbm")
    private Integer signalDbm;

    private Integer channel;

    @Column(length = 4)
    private String band;

    @Column(name = "radio_type", length = 20)
    private String radioType;

    @Column(nullable = false, length = 10)
    private String authentication = "UNKNOWN";

    @Column(name = "security_label", length = 40)
    private String securityLabel;

    @Column(length = 20)
    private String cipher;

    @Column(nullable = false)
    private boolean connected;

    protected WifiScanObservation() {
    }

    public WifiScanObservation(WifiScanSession scan, WifiAccessPoint accessPoint, Radio radio) {
        this.scan = scan;
        this.accessPoint = accessPoint;
        this.ssid = radio.ssid();
        this.signalPercent = radio.signalPercent();
        this.signalDbm = radio.signalDbm();
        this.channel = radio.channel();
        this.band = radio.band();
        this.radioType = radio.radioType();
        this.authentication = radio.authentication() == null ? "UNKNOWN" : radio.authentication();
        this.securityLabel = radio.securityLabel();
        this.cipher = radio.cipher();
        this.connected = radio.connected();
    }

    /** The radio side of an observation, as the agent read it. */
    public record Radio(String ssid, Integer signalPercent, Integer signalDbm, Integer channel, String band,
                        String radioType, String authentication, String securityLabel, String cipher,
                        boolean connected) {
    }

    public Long getId() { return id; }
    public WifiScanSession getScan() { return scan; }
    public WifiAccessPoint getAccessPoint() { return accessPoint; }
    public String getSsid() { return ssid; }
    public Integer getSignalPercent() { return signalPercent; }
    public Integer getSignalDbm() { return signalDbm; }
    public Integer getChannel() { return channel; }
    public String getBand() { return band; }
    public String getRadioType() { return radioType; }
    public String getAuthentication() { return authentication; }
    public String getSecurityLabel() { return securityLabel; }
    public String getCipher() { return cipher; }
    public boolean isConnected() { return connected; }
}
