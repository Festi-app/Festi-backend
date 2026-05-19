package com.festi.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.festi.backend.festival.Festival;
import java.time.LocalDate;
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


}
