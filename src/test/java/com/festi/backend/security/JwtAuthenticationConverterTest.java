package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festi.backend.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    @Test
    void convertsClaimsIntoAuthenticatedUserAndRoleAuthority() {
        UUID festivalId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("alice123")
                .claim("festivalId", festivalId.toString())
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AuthenticatedUserAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getPrincipal()).isEqualTo(
                new AuthenticatedUser("alice123", festivalId, UserRole.USER)
        );
        assertThat(authentication.getAuthorities())
                .contains(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void rejectsMissingRoleClaim() {
        Jwt jwt = jwtBuilder().build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsUnknownRoleClaim() {
        Jwt jwt = jwtBuilder()
                .claim("role", "ADMIN")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsBlankSubjectClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(" ")
                .claim("festivalId", UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsMissingFestivalIdClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("alice123")
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsInvalidFestivalIdClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("alice123")
                .claim("festivalId", "not-a-uuid")
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("alice123")
                .claim("festivalId", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}
