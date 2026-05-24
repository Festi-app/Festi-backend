package com.festi.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.festi.backend.user.UserDTO;
import com.festi.backend.user.UserRole;
import com.festi.backend.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class UserAdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

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
    void adminEndpointRequiresFestivalAdmin() throws Exception {
        when(userService.getUsersByRole(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users").param("role", "FESTIVAL_ADMIN"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users").param("role", "FESTIVAL_ADMIN")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").param("role", "FESTIVAL_ADMIN")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").param("role", "FESTIVAL_ADMIN")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanListUsersByRole() throws Exception {
        UserDTO.Response adminUser = new UserDTO.Response("admin1", festivalId, "Admin", "01011111111", UserRole.FESTIVAL_ADMIN);
        when(userService.getUsersByRole(eq(festivalId), eq(UserRole.FESTIVAL_ADMIN)))
                .thenReturn(List.of(adminUser));

        mockMvc.perform(get("/api/admin/users").param("role", "FESTIVAL_ADMIN")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("admin1"))
                .andExpect(jsonPath("$[0].role").value("FESTIVAL_ADMIN"));

        verify(userService).getUsersByRole(eq(festivalId), eq(UserRole.FESTIVAL_ADMIN));
    }

    private String token(UserRole role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(role.name().toLowerCase() + "user")
                .claim("festivalId", festivalId.toString())
                .claim("role", role.name())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}