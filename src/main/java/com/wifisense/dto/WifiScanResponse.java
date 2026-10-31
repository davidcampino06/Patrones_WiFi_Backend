package com.wifisense.dto;

import com.wifisense.model.WifiScanSession;

import java.time.Instant;

/**
 * One scan as the page shows it: its state, when it was asked for and finished, and, if it failed, why (the code and
 * a sentence for the user).
 */
public record WifiScanResponse(Long id, String origin, String status, Instant requestedAt, Instant startedAt,
                               Instant finishedAt, String failureReason, String failureMessage, String interfaceName,
                               String scanTool, Boolean fresh, Integer networkCount, Integer accessPointCount,
                               Integer durationMs) {

    public static WifiScanResponse from(WifiScanSession scan) {
        return new WifiScanResponse(scan.getId(), scan.getOrigin().name(), scan.getStatus().name(),
                scan.getRequestedAt(), scan.getStartedAt(), scan.getFinishedAt(),
                scan.getFailureReason() == null ? null : scan.getFailureReason().name(),
                scan.getFailureReason() == null ? null : scan.getFailureReason().message(), scan.getInterfaceName(),
                scan.getScanTool(), scan.getFresh(), scan.getNetworkCount(), scan.getAccessPointCount(),
                scan.getDurationMs());
    }
}
