package com.wifisense.dto;

import java.time.Instant;
import java.util.List;

/**
 * What WiFiSense knows about the router of a network and what it can (and cannot) do with it. Nothing here claims a
 * control that does not exist: listing the router's clients, blocking a device or changing the Wi-Fi password are
 * UNAVAILABLE until there is an official integration, and {@code steps} say how to do it in the router itself.
 */
public record RouterInfoResponse(boolean identified, String vendor, String model, String name, Instant lastSeenAt,
                                 int namesFromRouter, int confirmedNames, List<Capability> capabilities,
                                 List<String> steps) {

    /** READ_NAMES, LIST_CLIENTS, BLOCK_DEVICE or CHANGE_PASSWORD, with AVAILABLE or UNAVAILABLE and why. */
    public record Capability(String code, String label, String status, String detail) {
    }
}
