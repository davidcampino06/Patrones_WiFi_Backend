package com.wifisense.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("wifisense.ai")
public record AiServiceProperties(String url, String apiKey, @DefaultValue("10s") Duration timeout) {
}
