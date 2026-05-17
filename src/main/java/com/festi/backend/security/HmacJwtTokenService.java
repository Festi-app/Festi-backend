package com.festi.backend.security;

import com.festi.backend.auth.AuthDTO;
import com.festi.backend.user.User;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class HmacJwtTokenService implements JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final JwtProperties jwtProperties;

    public HmacJwtTokenService(JwtEncoder jwtEncoder, Clock clock, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthDTO.TokenResponse issueAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.accessTokenExpiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new AuthDTO.TokenResponse(token, "Bearer", jwtProperties.accessTokenExpiration());
    }
}
