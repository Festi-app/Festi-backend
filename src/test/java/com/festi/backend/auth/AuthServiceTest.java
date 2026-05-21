package com.festi.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.security.JwtTokenService;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, festivalRepository, passwordEncoder, jwtTokenService);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void signsUpNewUserWithHashedPasswordAndDefaultRole() {
        AuthDTO.SignupRequest request = new AuthDTO.SignupRequest(
                "alice123", "Password1!", "nickname", "01012345678");
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(userRepository.existsByIdAndFestivalId("alice123", festival.getId())).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(user.getId()).isEqualTo("alice123");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void rejectsDuplicateIdOnSignup() {
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(userRepository.existsByIdAndFestivalId("alice123", festival.getId())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new AuthDTO.SignupRequest(
                "alice123", "Password1!", "nickname", "01012345678")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void logsInWithValidCredentialsAndReturnsToken() {
        User user = user(festival, "alice123");
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(userRepository.findByIdAndFestivalId("alice123", festival.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hashed-password")).thenReturn(true);
        when(jwtTokenService.issueAccessToken(user))
                .thenReturn(new AuthDTO.TokenResponse("jwt", "Bearer", 3600));

        AuthDTO.TokenResponse response = authService.login(
                new AuthDTO.LoginRequest("alice123", "Password1!"));

        assertThat(response.accessToken()).isEqualTo("jwt");
    }

    @Test
    void rejectsInvalidCredentials() {
        User user = user(festival, "alice123");
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(userRepository.findByIdAndFestivalId("alice123", festival.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new AuthDTO.LoginRequest("alice123", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private User user(Festival festival, String id) {
        return new User(festival, id, "hashed-password", "nickname", "01012345678");
    }
}
