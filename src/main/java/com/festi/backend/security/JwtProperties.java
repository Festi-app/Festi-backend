package com.festi.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festi.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration
) {
}
