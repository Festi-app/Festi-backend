package com.festi.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.auth.AuthDTO;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.security.JwtTokenService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtTokenService);
    }

    @Test
    void updatesOnlyProvidedProfileFieldsAndIssuesFreshToken() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenService.issueAccessToken(user))
                .thenReturn(new AuthDTO.TokenResponse("new-token", "Bearer", 3600));

        UserDTO.UpdateResponse response = userService.updateMe(
                userId,
                new UserDTO.UpdateRequest(null, "new-name", null)
        );

        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().name()).isEqualTo("new-name");
        assertThat(response.user().phone()).isEqualTo("01012345678");
        assertThat(response.token().accessToken()).isEqualTo("new-token");
    }

    @Test
    void readsCurrentUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDTO.Response response = userService.getMe(userId);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("nickname");
        assertThat(response.phone()).isEqualTo("01012345678");
    }

    @Test
    void updatesPhoneOnly() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenService.issueAccessToken(user))
                .thenReturn(new AuthDTO.TokenResponse("new-token", "Bearer", 3600));

        UserDTO.UpdateResponse response = userService.updateMe(
                userId,
                new UserDTO.UpdateRequest(null, null, "01099998888")
        );

        assertThat(response.user().phone()).isEqualTo("01099998888");
    }

    @Test
    void updatesEmailOnly() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(jwtTokenService.issueAccessToken(user))
                .thenReturn(new AuthDTO.TokenResponse("new-token", "Bearer", 3600));

        UserDTO.UpdateResponse response = userService.updateMe(
                userId,
                new UserDTO.UpdateRequest("new@example.com", null, null)
        );

        assertThat(response.user().email()).isEqualTo("new@example.com");
    }

    @Test
    void updatesEmailAndRejectsDuplicateEmail() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("other@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateMe(
                userId,
                new UserDTO.UpdateRequest("other@example.com", null, null)
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void deletesCurrentUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteMe(userId);

        verify(userRepository).delete(user);
    }
}
