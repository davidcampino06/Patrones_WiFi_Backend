package com.wifisense.dto;

import com.wifisense.analysis.AnalysisType;

public record StrategyResponse(AnalysisType type, String description) {
}
