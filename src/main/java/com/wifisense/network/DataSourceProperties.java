package com.wifisense.network;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("wifisense.data-sources")
public record DataSourceProperties(@DefaultValue Simulation simulation, @DefaultValue SystemProbe systemProbe) {

    public record Simulation(@DefaultValue("0.1") double anomalyProbability) {
    }

    public record SystemProbe(@DefaultValue("1.1.1.1") String host,
                              @DefaultValue("443") int port,
                              @DefaultValue("5") int attempts,
                              @DefaultValue("1s") Duration timeout) {
    }
}
