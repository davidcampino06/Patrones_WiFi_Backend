package com.wifisense.dto;

import com.wifisense.model.Network;
import com.wifisense.network.DataSourceType;
import jakarta.validation.constraints.*;

public record NetworkRequest(
        @NotNull Long zoneId,
        @NotBlank @Size(max = 64) String ssid,
        @NotBlank @Pattern(regexp = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$", message = "must be a MAC address") String bssid,
        @NotBlank @Pattern(regexp = "^(2\\.4GHz|5GHz|6GHz)$") String frequencyBand,
        @Min(1) @Max(233) int channel,
        @NotNull Network.SecurityType securityType,
        @NotNull DataSourceType dataSourceType) {
}
