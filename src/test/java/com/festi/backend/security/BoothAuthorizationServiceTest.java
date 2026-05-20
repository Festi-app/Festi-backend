package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRole;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class BoothAuthorizationServiceTest {

    private final BoothAuthorizationService boothAuthorizationService = new BoothAuthorizationService();

    private Festival festival;

    @BeforeEach
    void setUp() {
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void festivalAdminsCanManageAnyBoothEvenWithoutAssignedManager() {
        Booth booth = boothWithoutManager();
        AuthenticatedUser festivalAdmin = authenticatedUser("admin", UserRole.FESTIVAL_ADMIN);

        assertThatCode(() -> boothAuthorizationService.assertCanManageBooth(festivalAdmin, booth))
                .doesNotThrowAnyException();
    }

    @Test
    void boothManagersCanManageTheirAssignedBooth() {
        User manager = user("manager1");
        Booth booth = boothWithManager(manager);
        AuthenticatedUser boothManager = authenticatedUser(manager.getId(), UserRole.BOOTH_MANAGER);

        assertThatCode(() -> boothAuthorizationService.assertCanManageBooth(boothManager, booth))
                .doesNotThrowAnyException();
    }

    @Test
    void boothManagersCannotManageAnotherManagersBooth() {
        Booth booth = boothWithManager(user("manager1"));
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
        User manager = user("user1");
        Booth booth = boothWithManager(manager);
        AuthenticatedUser regularUser = authenticatedUser(manager.getId(), UserRole.USER);

        assertThatThrownBy(() -> boothAuthorizationService.assertCanManageBooth(regularUser, booth))
                .isInstanceOf(AccessDeniedException.class);
    }

    private User user(String id) {
        return new User(festival, id, "hashed-pw", "name", "010-0000-0000");
    }

    private Booth boothWithManager(User manager) {
        Booth booth = boothWithoutManager();
        booth.assignManager(manager);
        return booth;
    }

    private Booth boothWithoutManager() {
        return new Booth("night booth", BoothCategory.ALCOHOL, BoothType.NIGHT, "creator");
    }

    private AuthenticatedUser authenticatedUser(String id, UserRole role) {
        return new AuthenticatedUser(id, UUID.randomUUID(), role);
    }
}
