package com.festi.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.festival.Festival;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void readsCurrentUserProfile() {
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");
        when(userRepository.findByIdAndFestivalId("alice123", festival.getId()))
                .thenReturn(Optional.of(user));

        UserDTO.Response response = userService.getMe("alice123", festival.getId());

        assertThat(response.id()).isEqualTo("alice123");
        assertThat(response.festivalId()).isEqualTo(festival.getId());
        assertThat(response.name()).isEqualTo("nickname");
        assertThat(response.phone()).isEqualTo("01012345678");
    }

    @Test
    void updatesOnlyProvidedProfileFields() {
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");
        when(userRepository.findByIdAndFestivalId("alice123", festival.getId()))
                .thenReturn(Optional.of(user));

        UserDTO.Response response = userService.updateMe(
                "alice123", festival.getId(),
                new UserDTO.UpdateRequest("new-name", null)
        );

        assertThat(response.id()).isEqualTo("alice123");
        assertThat(response.name()).isEqualTo("new-name");
        assertThat(response.phone()).isEqualTo("01012345678");
    }

    @Test
    void updatesPhoneOnly() {
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");
        when(userRepository.findByIdAndFestivalId("alice123", festival.getId()))
                .thenReturn(Optional.of(user));

        UserDTO.Response response = userService.updateMe(
                "alice123", festival.getId(),
                new UserDTO.UpdateRequest(null, "01099998888")
        );

        assertThat(response.phone()).isEqualTo("01099998888");
    }

    @Test
    void listsUsersByRole() {
        User admin = new User(festival, "admin1", "hashed", "Admin", "01011111111");
        admin.changeRole(UserRole.FESTIVAL_ADMIN);
        when(userRepository.findByFestivalIdAndRole(festival.getId(), UserRole.FESTIVAL_ADMIN))
                .thenReturn(List.of(admin));

        List<UserDTO.Response> result = userService.getUsersByRole(festival.getId(), UserRole.FESTIVAL_ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("admin1");
        assertThat(result.get(0).role()).isEqualTo(UserRole.FESTIVAL_ADMIN);
    }

    @Test
    void updatesUserRole() {
        User user = new User(festival, "manager1", "hashed", "Manager", "01022222222");
        when(userRepository.findByIdAndFestivalId("manager1", festival.getId()))
                .thenReturn(Optional.of(user));

        UserDTO.Response response = userService.updateUserRole("manager1", festival.getId(), UserRole.FESTIVAL_ADMIN);

        assertThat(response.role()).isEqualTo(UserRole.FESTIVAL_ADMIN);
    }

    @Test
    void rejectsSettingRoleToUser() {
        assertThatThrownBy(() -> userService.updateUserRole("manager1", festival.getId(), UserRole.USER))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetsUserRoleToUser() {
        User user = new User(festival, "admin1", "hashed", "Admin", "01033333333");
        user.changeRole(UserRole.FESTIVAL_ADMIN);
        when(userRepository.findByIdAndFestivalId("admin1", festival.getId()))
                .thenReturn(Optional.of(user));

        UserDTO.Response response = userService.resetUserRole("admin1", festival.getId());

        assertThat(response.role()).isEqualTo(UserRole.USER);
    }
}
