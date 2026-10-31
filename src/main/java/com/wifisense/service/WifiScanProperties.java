package com.wifisense.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Limits of the scans of nearby Wi-Fi networks, all configurable ({@code wifisense.scan.*}).
 * <ul>
 *   <li>{@code scheduledInterval}: the server asks the agent for a scan when the last one is older than this.</li>
 *   <li>{@code manualMinInterval}: "Escanear ahora" cannot be pressed more often than this.</li>
 *   <li>{@code agentTimeout}: a scan the agent did not finish in this time fails as AGENT_NO_RESPONSE (the agent
 *       asks for pending scans every 30 seconds and a scan takes a few).</li>
 *   <li>{@code pollMinInterval}: an agent cannot ask for pending scans more often than this.</li>
 *   <li>{@code staleAfter}: the page marks the last result as out of date after this.</li>
 *   <li>{@code maxAccessPoints}: the strongest ones kept from a single scan.</li>
 *   <li>{@code retention} and {@code accessPointRetention}: scans and access points not seen for longer are deleted
 *       every night.</li>
 * </ul>
 */
@ConfigurationProperties("wifisense.scan")
public record WifiScanProperties(
        @DefaultValue("60m") Duration scheduledInterval,
        @DefaultValue("30s") Duration manualMinInterval,
        @DefaultValue("3m") Duration agentTimeout,
        @DefaultValue("10s") Duration pollMinInterval,
        @DefaultValue("150m") Duration staleAfter,
        @DefaultValue("200") int maxAccessPoints,
        @DefaultValue("30d") Duration retention,
        @DefaultValue("90d") Duration accessPointRetention) {
}
