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
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import com.festi.backend.user.UserDTO;
import com.festi.backend.user.UserRole;
import com.festi.backend.user.UserService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    private UUID festivalId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        festivalId = UUID.randomUUID();
    }

    @Test
    void signupAndLoginArePublic() throws Exception {
        Festival festival = festival(festivalId);
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");
        when(authService.signup(any())).thenReturn(user);
        when(authService.login(any())).thenReturn(new AuthDTO.TokenResponse("jwt", "Bearer", 3600));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDTO.SignupRequest(
                                "alice123", "Password1!", "nickname", "01012345678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("alice123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthDTO.LoginRequest("alice123", "Password1!"))))
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
        when(userService.getMe(eq("alice123"), any()))
                .thenReturn(new UserDTO.Response("alice123", "nickname", "01012345678", UserRole.USER));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token("alice123", festivalId, Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("alice123"));
    }

    @Test
    void patchUpdatesProfileAndReturnsResponse() throws Exception {
        String tok = token("alice123", festivalId, Instant.now().plusSeconds(3600));
        when(userService.updateMe(eq("alice123"), any(), any()))
                .thenReturn(new UserDTO.Response("alice123", "new-name", "01012345678", UserRole.USER));

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"new-name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new-name"));
    }

    @Test
    void rejectsExpiredAndWronglySignedTokens() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token(
                                "alice123", festivalId, Instant.now().minusSeconds(60))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenWithDifferentSecret("alice123", festivalId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokensWithMissingOrInvalidRequiredClaims() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "alice123", festivalId.toString(), null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "alice123", festivalId.toString(), "ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "alice123", "not-a-uuid", "USER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "alice123", null, "USER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private String token(String userId, UUID festivalId, Instant expiresAt) {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
                ? expiresAt.minusSeconds(3600)
                : Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("festivalId", festivalId.toString())
                .claim("role", "USER")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String tokenWithDifferentSecret(String userId, UUID festivalId) {
        SecretKey secretKey = new SecretKeySpec(
                "different-secret-that-is-at-least-32-bytes".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("festivalId", festivalId.toString())
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String tokenWithClaims(String subject, String festivalId, String role) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (festivalId != null) {
            builder.claim("festivalId", festivalId);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
    }

    private Festival festival(UUID id) {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }
}
