package com.wifisense.dto;

import com.wifisense.model.WifiAccessPoint;
import com.wifisense.model.WifiScanObservation;

import java.time.Instant;

/**
 * A nearby access point as one scan saw it, plus its history (first seen, how many scans saw it). Seeing a network
 * does not make it the user's: {@code connected} only says it is the one the agent's computer was connected to.
 * Unknown values are null, never guessed.
 */
public record NearbyAccessPoint(Long id, String ssid, boolean hidden, String bssidSuffix, String vendor,
                                Integer signalPercent, Integer signalDbm, Integer channel, String band,
                                String radioType, String authentication, String securityLabel, String cipher,
                                boolean connected, Instant firstSeenAt, int timesSeen) {

    public static NearbyAccessPoint from(WifiScanObservation observation) {
        WifiAccessPoint ap = observation.getAccessPoint();
        return new NearbyAccessPoint(ap.getId(), observation.getSsid(), observation.getSsid() == null,
                ap.getBssidSuffix(), ap.getVendor(), observation.getSignalPercent(), observation.getSignalDbm(),
                observation.getChannel(), observation.getBand(), observation.getRadioType(),
                observation.getAuthentication(), observation.getSecurityLabel(), observation.getCipher(),
                observation.isConnected(), ap.getFirstSeenAt(), ap.getTimesSeen());
    }
}
