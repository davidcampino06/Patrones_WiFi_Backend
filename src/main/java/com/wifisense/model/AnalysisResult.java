package com.wifisense.model;

import com.wifisense.analysis.AnalysisType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "analysis_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 30)
    private AnalysisType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_status", nullable = false, length = 20)
    private NetworkStatus detectedStatus;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "measurement_count", nullable = false)
    private int measurementCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AnalysisResult() {
    }

    public AnalysisResult(Network network, User requestedBy, AnalysisType type, NetworkStatus detectedStatus,
                          double score, String summary, int measurementCount) {
        this.network = network;
        this.requestedBy = requestedBy;
        this.type = type;
        this.detectedStatus = detectedStatus;
        this.score = score;
        this.summary = summary;
        this.measurementCount = measurementCount;
    }

    public Long getId() { return id; }
    public Network getNetwork() { return network; }
    public User getRequestedBy() { return requestedBy; }
    public AnalysisType getType() { return type; }
    public NetworkStatus getDetectedStatus() { return detectedStatus; }
    public double getScore() { return score; }
    public String getSummary() { return summary; }
    public int getMeasurementCount() { return measurementCount; }
    public Instant getCreatedAt() { return createdAt; }
}
