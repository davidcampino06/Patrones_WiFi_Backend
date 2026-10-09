package com.wifisense.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Administrator credentials come only from environment variables, never from the repository. */
@ConfigurationProperties("wifisense.accounts")
public record AccountProperties(String adminUsername, String adminPassword, @DefaultValue("3") int maxUsers) {
}
