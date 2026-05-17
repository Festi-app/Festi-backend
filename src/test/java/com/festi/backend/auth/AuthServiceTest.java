package com.festi.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.security.JwtTokenService;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void signsUpNewUserWithHashedPasswordAndDefaultRole() {
        AuthDTO.SignupRequest request = new AuthDTO.SignupRequest(
                "user@example.com", "Password1!", "nickname", "01012345678");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void rejectsDuplicateEmailOnSignup() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new AuthDTO.SignupRequest(
                "user@example.com", "Password1!", "nickname", "01012345678")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void logsInWithValidCredentialsAndReturnsToken() {
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hashed-password")).thenReturn(true);
        when(jwtTokenService.issueAccessToken(user))
                .thenReturn(new AuthDTO.TokenResponse("jwt", "Bearer", 3600));

        AuthDTO.TokenResponse response = authService.login(
                new AuthDTO.LoginRequest("user@example.com", "Password1!"));

        assertThat(response.accessToken()).isEqualTo("jwt");
    }

    @Test
    void rejectsInvalidCredentials() {
        User user = new User("user@example.com", "hashed-password", "nickname", "01012345678");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new AuthDTO.LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
