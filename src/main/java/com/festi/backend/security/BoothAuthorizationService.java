package com.festi.backend.security;

import com.festi.backend.booth.Booth;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRole;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class BoothAuthorizationService {

    public void assertCanManageBooth(AuthenticatedUser currentUser, Booth booth) {
        if (currentUser.role() == UserRole.FESTIVAL_ADMIN) {
            return;
        }

        User manager = booth.getManager();
        if (currentUser.role() != UserRole.BOOTH_MANAGER
                || manager == null
                || !Objects.equals(manager.getId(), currentUser.id())) {
            throw new AccessDeniedException("Access is denied.");
        }
    }
}
