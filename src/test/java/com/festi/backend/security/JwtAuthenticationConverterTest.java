package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    @Test
    void convertsClaimsIntoAuthenticatedUserAndRoleAuthority() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("email", "user@example.com")
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AuthenticatedUserAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getPrincipal()).isEqualTo(
                new AuthenticatedUser(userId, "user@example.com", UserRole.USER)
        );
        assertThat(authentication.getAuthorities())
                .contains(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
