package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.auth.AuthDTO;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class HmacJwtTokenServiceTest {

    @Test
    void issuesAccessTokenWithConfiguredClaims() {
        String secret = "test-secret-that-is-at-least-32-bytes-long";
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Clock clock = Clock.fixed(Instant.parse("2030-05-17T00:00:00Z"), ZoneOffset.UTC);
        JwtProperties properties = new JwtProperties(secret, 3600);
        HmacJwtTokenService tokenService = new HmacJwtTokenService(
                NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build(),
                clock,
                properties
        );
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        UUID festivalId = UUID.randomUUID();
        ReflectionTestUtils.setField(festival, "id", festivalId);
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");

        AuthDTO.TokenResponse token = tokenService.issueAccessToken(user);
        Jwt decoded = decoder.decode(token.accessToken());

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(3600);
        assertThat(decoded.getSubject()).isEqualTo("alice123");
        assertThat(decoded.getClaimAsString("festivalId")).isEqualTo(festivalId.toString());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("USER");
        assertThat(decoded.getIssuedAt()).isEqualTo(Instant.parse("2030-05-17T00:00:00Z"));
        assertThat(decoded.getExpiresAt()).isEqualTo(Instant.parse("2030-05-17T01:00:00Z"));
    }
}
