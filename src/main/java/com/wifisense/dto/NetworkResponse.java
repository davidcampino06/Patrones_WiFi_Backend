package com.wifisense.dto;

import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import com.wifisense.network.DataSourceType;

import java.time.Instant;

public record NetworkResponse(Long id, String ssid, String bssid, String frequencyBand, int channel,
                              Network.SecurityType securityType, NetworkStatus status,
                              DataSourceType dataSourceType, Long zoneId, String zoneName, String locationName,
                              Instant lastCollectedAt) {

    public static NetworkResponse from(Network n) {
        return new NetworkResponse(n.getId(), n.getSsid(), n.getBssid(), n.getFrequencyBand(), n.getChannel(),
                n.getSecurityType(), n.getStatus(), n.getDataSourceType(), n.getZone().getId(),
                n.getZone().getName(), n.getZone().getLocation().getName(), n.getLastCollectedAt());
    }
}
