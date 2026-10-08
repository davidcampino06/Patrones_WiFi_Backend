package com.wifisense.dto;

import com.wifisense.model.Device;
import jakarta.validation.constraints.*;

public record DeviceRequest(
        @NotNull Long networkId,
        @Size(max = 100) String hostname,
        @NotBlank @Size(max = 45) String ipAddress,
        @NotBlank @Pattern(regexp = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$", message = "must be a MAC address") String macAddress,
        @NotNull Device.Type type,
        @Min(-100) @Max(0) Integer signalStrength) {
}
