package com.wifisense.analysis;

/** Strategy: interchangeable ways of judging a network's health from its measurement window. */
public interface AnalysisStrategy {

    AnalysisType type();

    String description();

    AnalysisOutcome analyze(AnalysisContext context);
}
