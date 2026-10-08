package com.wifisense.model;

import com.wifisense.analysis.AiAnalysisResponse;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_predictions")
public class AiPrediction {

    public enum Severity { NONE, LOW, MEDIUM, HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_result_id", nullable = false, unique = true)
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    @Column(name = "anomaly_detected", nullable = false)
    private boolean anomalyDetected;

    @Column(name = "anomaly_score", nullable = false)
    private double anomalyScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(length = 500)
    private String recommendation;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "simulated_data", nullable = false)
    private boolean simulatedData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiPrediction() {
    }

    public static AiPrediction from(AnalysisResult result, AiAnalysisResponse response) {
        AiPrediction prediction = new AiPrediction();
        prediction.analysisResult = result;
        prediction.network = result.getNetwork();
        prediction.anomalyDetected = response.anomalyDetected();
        prediction.anomalyScore = response.anomalyScore();
        prediction.severity = response.severity();
        prediction.message = response.message();
        prediction.recommendation = response.recommendation();
        prediction.modelVersion = response.modelVersion();
        prediction.simulatedData = response.simulatedData();
        return prediction;
    }

    public Long getId() { return id; }
    public AnalysisResult getAnalysisResult() { return analysisResult; }
    public Network getNetwork() { return network; }
    public boolean isAnomalyDetected() { return anomalyDetected; }
    public double getAnomalyScore() { return anomalyScore; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getRecommendation() { return recommendation; }
    public String getModelVersion() { return modelVersion; }
    public boolean isSimulatedData() { return simulatedData; }
    public Instant getCreatedAt() { return createdAt; }
}
