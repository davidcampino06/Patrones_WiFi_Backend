package com.wifisense.analysis;

/** Abstraction over the AI service so the analysis does not depend on HTTP details (DIP). */
public interface AiAnalysisClient {

    AiAnalysisResponse detectAnomalies(AiAnalysisRequest request);
}
