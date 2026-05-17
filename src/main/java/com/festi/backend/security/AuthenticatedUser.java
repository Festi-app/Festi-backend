package com.festi.backend.security;

import com.festi.backend.user.UserRole;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        UserRole role
) {
}
