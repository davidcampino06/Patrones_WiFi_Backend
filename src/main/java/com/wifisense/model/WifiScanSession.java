package com.wifisense.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One scan of the Wi-Fi networks around the agent of a network of Mi Wi-Fi. It is created PENDING (by the user or by
 * the schedule), moves to RUNNING when the agent picks it up and ends COMPLETED or FAILED. The moves are checked
 * here, so no code path can, for example, complete a scan that already failed.
 */
@Entity
@Table(name = "wifi_scan_sessions")
public class WifiScanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_network_id", nullable = false, updatable = false)
    private HomeNetwork network;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private ScanOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScanStatus status = ScanStatus.PENDING;

    /** Who pressed "Escanear ahora"; null for scheduled scans (and forgotten if the account is deleted). */
    @Column(name = "requested_by", updatable = false)
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 24)
    private ScanFailure failureReason;

    @Column(name = "interface_name", length = 64)
    private String interfaceName;

    @Column(name = "scan_tool", length = 8)
    private String scanTool;

    private Boolean fresh;

    @Column(name = "network_count")
    private Integer networkCount;

    @Column(name = "access_point_count")
    private Integer accessPointCount;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "agent_version", length = 20)
    private String agentVersion;

    protected WifiScanSession() {
    }

    public WifiScanSession(HomeNetwork network, ScanOrigin origin, Long requestedBy, Instant requestedAt) {
        this.network = network;
        this.origin = origin;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    /** The agent picked the scan up. */
    public void start(Instant now) {
        move(ScanStatus.RUNNING);
        this.startedAt = later(now);
    }

    /** What the agent saw; zero networks is a valid result (nothing in range, or a shielded room). */
    public void complete(Instant now, ScanDetails details, int networks, int accessPoints) {
        move(ScanStatus.COMPLETED);
        describe(details);
        this.networkCount = networks;
        this.accessPointCount = accessPoints;
        this.finishedAt = later(now);
        if (startedAt == null) {
            startedAt = finishedAt;
        }
    }

    public void fail(Instant now, ScanFailure reason, ScanDetails details) {
        move(ScanStatus.FAILED);
        if (details != null) {
            describe(details);
        }
        this.failureReason = reason;
        this.finishedAt = later(now);
    }

    private void describe(ScanDetails details) {
        this.interfaceName = details.interfaceName();
        this.scanTool = details.scanTool();
        this.fresh = details.fresh();
        this.durationMs = details.durationMs();
        this.agentVersion = details.agentVersion();
    }

    private void move(ScanStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Ese escaneo ya terminó.");
        }
        this.status = next;
    }

    /** A clock that went back must not break the "never before it was requested" rule of the table. */
    private Instant later(Instant now) {
        return now.isBefore(requestedAt) ? requestedAt : now;
    }

    /** How the agent scanned: adapter, tool, whether it asked the system for a fresh scan, and how long it took. */
    public record ScanDetails(String interfaceName, String scanTool, Boolean fresh, Integer durationMs,
                              String agentVersion) {
    }

    public Long getId() { return id; }
    public HomeNetwork getNetwork() { return network; }
    public ScanOrigin getOrigin() { return origin; }
    public ScanStatus getStatus() { return status; }
    public Long getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public ScanFailure getFailureReason() { return failureReason; }
    public String getInterfaceName() { return interfaceName; }
    public String getScanTool() { return scanTool; }
    public Boolean getFresh() { return fresh; }
    public Integer getNetworkCount() { return networkCount; }
    public Integer getAccessPointCount() { return accessPointCount; }
    public Integer getDurationMs() { return durationMs; }
    public String getAgentVersion() { return agentVersion; }
}
