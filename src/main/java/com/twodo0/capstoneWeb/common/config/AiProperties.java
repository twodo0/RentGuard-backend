package com.twodo0.capstoneWeb.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        boolean mock,
        String baseUrl,
        int timeoutSeconds,
        double thresholdDefault,
        Endpoints endpoints
        ) {
    public record Endpoints(String predict){}
}