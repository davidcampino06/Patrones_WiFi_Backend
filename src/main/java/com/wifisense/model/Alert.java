package com.wifisense.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {

    public enum Severity { INFO, WARNING, CRITICAL }

    public enum Status { OPEN, ACKNOWLEDGED, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_result_id")
    private AnalysisResult analysisResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Alert() {
    }

    public Alert(Network network, AnalysisResult analysisResult, Severity severity, String message) {
        this.network = network;
        this.analysisResult = analysisResult;
        this.severity = severity;
        this.message = message;
    }

    public void acknowledge() {
        if (status != Status.OPEN) {
            throw new IllegalStateException("Solo se pueden reconocer alertas abiertas");
        }
        status = Status.ACKNOWLEDGED;
    }

    public void resolve(Instant at) {
        if (status == Status.RESOLVED) {
            throw new IllegalStateException("La alerta ya está resuelta");
        }
        status = Status.RESOLVED;
        resolvedAt = at;
    }

    public Long getId() { return id; }
    public Network getNetwork() { return network; }
    public AnalysisResult getAnalysisResult() { return analysisResult; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
