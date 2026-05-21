package com.festi.backend.security;

import com.festi.backend.user.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.util.StringUtils;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AuthenticatedUserAuthenticationToken> {

    @Override
    public AuthenticatedUserAuthenticationToken convert(Jwt jwt) {
        UserRole role = parseRole(jwt.getClaimAsString("role"));
        AuthenticatedUser principal = new AuthenticatedUser(
                requireUserId(jwt.getSubject()),
                parseFestivalId(jwt.getClaimAsString("festivalId")),
                role
        );

        return new AuthenticatedUserAuthenticationToken(
                principal,
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private UserRole parseRole(String roleClaim) {
        if (!StringUtils.hasText(roleClaim)) {
            throw invalidToken("JWT role claim is missing.");
        }
        try {
            return UserRole.valueOf(roleClaim);
        } catch (IllegalArgumentException e) {
            throw invalidToken("JWT role claim is invalid.");
        }
    }

    private String requireUserId(String subject) {
        if (!StringUtils.hasText(subject)) {
            throw invalidToken("JWT subject claim is missing.");
        }
        return subject;
    }

    private UUID parseFestivalId(String festivalIdClaim) {
        if (!StringUtils.hasText(festivalIdClaim)) {
            throw invalidToken("JWT festivalId claim is missing.");
        }
        try {
            return UUID.fromString(festivalIdClaim);
        } catch (IllegalArgumentException e) {
            throw invalidToken("JWT festivalId claim is invalid.");
        }
    }

    private InvalidBearerTokenException invalidToken(String message) {
        return new InvalidBearerTokenException(message);
    }
}
