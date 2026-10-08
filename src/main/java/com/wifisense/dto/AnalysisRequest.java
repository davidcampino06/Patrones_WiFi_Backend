package com.wifisense.dto;

import com.wifisense.analysis.AnalysisType;
import jakarta.validation.constraints.NotNull;

public record AnalysisRequest(@NotNull AnalysisType type) {
}
