package com.wifisense.dto;

import java.time.Instant;
import java.util.List;

/**
 * The "Redes cercanas" panel of a network: whether its agent is installed and connected, the newest scan (whatever
 * its state, so the page can show "escaneando…" or why it failed), the last completed scan with what it saw, and
 * whether that result is already old ({@code stale}).
 */
public record NearbyNetworksResponse(boolean agentInstalled, boolean agentOnline, WifiScanResponse current,
                                     WifiScanResponse lastCompleted, boolean stale, long staleAfterMinutes,
                                     Instant nextScheduledAt, Instant manualAvailableAt,
                                     List<NearbyAccessPoint> accessPoints) {
}
