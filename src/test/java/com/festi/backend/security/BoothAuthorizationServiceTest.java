package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class BoothAuthorizationServiceTest {

    private final BoothAuthorizationService boothAuthorizationService = new BoothAuthorizationService();

    @Test
    void festivalAdminsCanManageAnyBoothEvenWithoutAssignedManager() {
        Booth booth = boothWithoutManager();
        AuthenticatedUser festivalAdmin = authenticatedUser(UserRole.FESTIVAL_ADMIN);

        assertThatCode(() -> boothAuthorizationService.assertCanManageBooth(festivalAdmin, booth))
                .doesNotThrowAnyException();
    }

    @Test
    void boothManagersCanManageTheirAssignedBooth() {
        UUID managerId = UUID.randomUUID();
        Booth booth = boothWithManager(managerId);
        AuthenticatedUser boothManager = authenticatedUser(managerId, UserRole.BOOTH_MANAGER);

        assertThatCode(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .doesNotThrowAnyException();
    }

    @Test
    void boothManagersCannotManageAnotherManagersBooth() {
        Booth booth = boothWithManager(UUID.randomUUID());
        AuthenticatedUser boothManager = authenticatedUser(UserRole.BOOTH_MANAGER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void boothManagersCannotManageBoothsWithoutAssignedManager() {
        Booth booth = boothWithoutManager();
        AuthenticatedUser boothManager = authenticatedUser(UserRole.BOOTH_MANAGER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void regularUsersCannotManageBoothsEvenWhenTheyAreAssignedManager() {
        UUID managerId = UUID.randomUUID();
        Booth booth = boothWithManager(managerId);
        AuthenticatedUser user = authenticatedUser(managerId, UserRole.USER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(user, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Booth boothWithManager(UUID managerId) {
        Booth booth = boothWithoutManager();
        booth.assignManager(user(managerId));
        return booth;
    }

    private Booth boothWithoutManager() {
        return new Booth("night booth", BoothCategory.ALCOHOL, BoothType.NIGHT, user(UUID.randomUUID()));
    }

    private AuthenticatedUser authenticatedUser(UserRole role) {
        return authenticatedUser(UUID.randomUUID(), role);
    }

    private AuthenticatedUser authenticatedUser(UUID id, UserRole role) {
        return new AuthenticatedUser(id, role.name().toLowerCase() + "@example.com", role);
    }

    private User user(UUID id) {
        User user = new User("user-" + id + "@example.com", "hashed-password", "nickname", "01012345678");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
