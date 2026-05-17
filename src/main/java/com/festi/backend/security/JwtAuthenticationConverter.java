package com.festi.backend.security;

import com.festi.backend.user.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AuthenticatedUserAuthenticationToken> {

    @Override
    public AuthenticatedUserAuthenticationToken convert(Jwt jwt) {
        UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                role
        );

        return new AuthenticatedUserAuthenticationToken(
                principal,
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
