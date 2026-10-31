package com.wifisense.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What the AI concluded about the last scan of the nearby networks: facts (counted in the scan) apart from inferences
 * (possible explanations, each with its confidence), and the uncertainty of the whole reading. {@code available} is
 * false when there is no completed scan or the AI is down: then {@code message} says so and nothing is invented.
 */
public record NearbyAnalysisResponse(Long scanId, Instant scannedAt, boolean available, String message,
                                     List<Item> facts, List<Item> inferences, String uncertainty) {

    public record Item(String code, String severity, String title, String evidence, String recommendation,
                       BigDecimal confidence) {
    }

    public static NearbyAnalysisResponse unavailable(Long scanId, Instant scannedAt, String message) {
        return new NearbyAnalysisResponse(scanId, scannedAt, false, message, List.of(), List.of(), null);
    }
}
