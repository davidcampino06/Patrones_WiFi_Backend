package com.wifisense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * What the agent saw in a scan the server asked for, or why it could not scan. The agent sends each BSSID only as a
 * fingerprint with its own salt plus the last byte; anything out of range is rejected so a forged result cannot
 * store nonsense.
 */
public record AgentScanResult(
        @NotNull(message = "Falta el escaneo.") Long scanId,
        @NotNull @Pattern(regexp = "^(COMPLETED|FAILED)$", message = "Estado de escaneo desconocido.") String status,
        @Pattern(regexp = "^(NO_ADAPTER|ADAPTER_OFF|SERVICE_OFF|LOCATION_PERMISSION|COMMAND_FAILED|TIMEOUT"
                + "|UNREADABLE_OUTPUT|UNSUPPORTED_OS)$", message = "Motivo de falla desconocido.") String failureReason,
        @Size(max = 64) String interfaceName,
        @Pattern(regexp = "^(NETSH|NMCLI)$", message = "Herramienta de escaneo desconocida.") String scanTool,
        Boolean fresh,
        @Min(0) @Max(600_000) Integer durationMs,
        @Pattern(regexp = "^[0-9A-Za-z.+-]{1,20}$", message = "Versión del agente inválida.") String agentVersion,
        @Size(max = 300, message = "Demasiados puntos de acceso en un escaneo.") @Valid List<AccessPoint> accessPoints) {

    public List<AccessPoint> accessPointsOrEmpty() {
        return accessPoints == null ? List.of() : accessPoints;
    }

    /** One access point: fingerprint of its BSSID, what the system said about it, and nothing invented. */
    public record AccessPoint(
            @NotNull @Pattern(regexp = "^[0-9a-f]{24}$", message = "Huella de BSSID inválida.") String bssid,
            @Pattern(regexp = "^[0-9A-F]{2}$", message = "Final de BSSID inválido.") String bssidSuffix,
            @Size(max = 60) String vendor,
            @Size(max = 64, message = "El nombre de la red es demasiado largo.") String ssid,
            @Min(0) @Max(100) Integer signalPercent,
            @Min(-120) @Max(0) Integer signalDbm,
            @Min(1) @Max(233) Integer channel,
            @Pattern(regexp = "^(2\\.4|5|6)$", message = "Banda desconocida.") String band,
            @Size(max = 20) String radioType,
            @Pattern(regexp = "^(OPEN|WEP|WPA|WPA2|WPA3|UNKNOWN)$", message = "Tipo de seguridad desconocido.")
            String authentication,
            @Size(max = 40) String securityLabel,
            @Size(max = 20) String cipher,
            Boolean connected) {
    }
}
