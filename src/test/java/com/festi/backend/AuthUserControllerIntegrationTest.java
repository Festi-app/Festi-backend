package com.festi.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.festi.backend.auth.AuthDTO;
import com.festi.backend.auth.AuthService;
import com.festi.backend.user.User;
import com.festi.backend.user.UserDTO;
import com.festi.backend.user.UserRole;
import com.festi.backend.user.UserService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class AuthUserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void signupAndLoginArePublic() throws Exception {
        User user = user(UUID.randomUUID(), "user@example.com");
        when(authService.signup(any())).thenReturn(user);
        when(authService.login(any())).thenReturn(new AuthDTO.TokenResponse("jwt", "Bearer", 3600));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDTO.SignupRequest(
                                "user@example.com", "Password1!", "nickname", "01012345678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthDTO.LoginRequest("user@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"));
    }

    @Test
    void meEndpointRejectsMissingTokenWithErrorResponse() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authenticatedUserCanReadOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getMe(userId))
                .thenReturn(new UserDTO.Response(userId, "user@example.com", "nickname", "01012345678", UserRole.USER));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token(userId, "user@example.com", Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void patchReturnsFreshTokenAndOldTokenRemainsUsableUntilExpiry() throws Exception {
        UUID userId = UUID.randomUUID();
        String oldToken = token(userId, "old@example.com", Instant.now().plusSeconds(3600));
        String newToken = token(userId, "new@example.com", Instant.now().plusSeconds(3600));
        when(userService.updateMe(eq(userId), any()))
                .thenReturn(new UserDTO.UpdateResponse(
                        new UserDTO.Response(userId, "new@example.com", "nickname", "01012345678", UserRole.USER),
                        new AuthDTO.TokenResponse(newToken, "Bearer", 3600)
                ));
        when(userService.getMe(userId))
                .thenReturn(new UserDTO.Response(userId, "new@example.com", "nickname", "01012345678", UserRole.USER));

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("new@example.com"))
                .andExpect(jsonPath("$.token.accessToken").value(newToken));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void rejectsExpiredAndWronglySignedTokens() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token(
                                userId, "user@example.com", Instant.now().minusSeconds(60))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenWithDifferentSecret(
                                userId, "user@example.com")))
                .andExpect(status().isUnauthorized());
    }

    private String token(UUID userId, String email, Instant expiresAt) {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
                ? expiresAt.minusSeconds(3600)
                : Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", "USER")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String tokenWithDifferentSecret(UUID userId, String email) {
        SecretKey secretKey = new SecretKeySpec(
                "different-secret-that-is-at-least-32-bytes".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private User user(UUID id, String email) {
        User user = new User(email, "hashed-password", "nickname", "01012345678");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
