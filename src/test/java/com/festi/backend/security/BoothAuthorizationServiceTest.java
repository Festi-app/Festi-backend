package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class BoothAuthorizationServiceTest {

    private final BoothAuthorizationService boothAuthorizationService = new BoothAuthorizationService();

    @Test
    void festivalAdminsCanManageAnyBoothEvenWithoutAssignedManager() {
        Booth booth = boothWithoutManager();
        AuthenticatedUser festivalAdmin = authenticatedUser("admin", UserRole.FESTIVAL_ADMIN);

        assertThatCode(() -> boothAuthorizationService.assertCanManageBooth(festivalAdmin, booth))
                .doesNotThrowAnyException();
    }

    @Test
    void boothManagersCanManageTheirAssignedBooth() {
        Booth booth = boothWithManager("manager1");
        AuthenticatedUser boothManager = authenticatedUser("manager1", UserRole.BOOTH_MANAGER);

        assertThatCode(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .doesNotThrowAnyException();
    }

    @Test
    void boothManagersCannotManageAnotherManagersBooth() {
        Booth booth = boothWithManager("manager1");
        AuthenticatedUser boothManager = authenticatedUser("manager2", UserRole.BOOTH_MANAGER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void boothManagersCannotManageBoothsWithoutAssignedManager() {
        Booth booth = boothWithoutManager();
        AuthenticatedUser boothManager = authenticatedUser("manager1", UserRole.BOOTH_MANAGER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void regularUsersCannotManageBoothsEvenWhenTheyAreAssignedManager() {
        Booth booth = boothWithManager("user1");
        AuthenticatedUser user = authenticatedUser("user1", UserRole.USER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(user, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Booth boothWithManager(String managerId) {
        Booth booth = boothWithoutManager();
        booth.assignManager(managerId);
        return booth;
    }

    private Booth boothWithoutManager() {
        return new Booth("night booth", BoothCategory.ALCOHOL, BoothType.NIGHT, "creator");
    }

    private AuthenticatedUser authenticatedUser(String id, UserRole role) {
        return new AuthenticatedUser(id, UUID.randomUUID(), role);
    }
}
