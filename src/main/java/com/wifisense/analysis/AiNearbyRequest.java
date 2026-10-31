package com.wifisense.analysis;

import java.util.List;

/**
 * Contract of POST /api/v1/nearby/analyze in WiFiSense-ai: the access points of one scan of the nearby networks, as
 * the agent saw them (no BSSID: only its last byte, and the maker). Unknown values stay null.
 */
public record AiNearbyRequest(List<AccessPoint> accessPoints) {

    public record AccessPoint(String ssid, boolean hidden, String authentication, String cipher, Integer channel,
                              String band, Integer signalPercent, Integer signalDbm, boolean connected,
                              String vendor) {
    }
}
