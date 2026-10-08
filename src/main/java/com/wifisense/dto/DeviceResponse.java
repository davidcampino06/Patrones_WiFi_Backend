package com.wifisense.dto;

import com.wifisense.model.Device;

import java.time.Instant;

public record DeviceResponse(Long id, Long networkId, String networkSsid, String hostname, String ipAddress,
                             String macAddress, Device.Type type, Device.ConnectionStatus connectionStatus,
                             Integer signalStrength, Instant lastSeenAt) {

    public static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getNetwork().getId(), d.getNetwork().getSsid(), d.getHostname(),
                d.getIpAddress(), d.getMacAddress(), d.getType(), d.getConnectionStatus(), d.getSignalStrength(),
                d.getLastSeenAt());
    }
}
