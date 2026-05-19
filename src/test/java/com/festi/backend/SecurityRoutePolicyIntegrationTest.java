package com.festi.backend;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.festi.backend.booth.BoothService;
import com.festi.backend.user.UserRole;
import com.festi.backend.waiting.WaitingService;
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
class SecurityRoutePolicyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private BoothService boothService;

    @MockitoBean
    private WaitingService waitingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void userLevelReadRoutesRejectMissingAuthentication() throws Exception {
        String[] routes = {
                "/api/booths",
                "/api/booths/" + UUID.randomUUID(),
                "/api/booths/" + UUID.randomUUID() + "/menus",
                "/api/locations",
                "/api/festival",
                "/api/festival/notices"
        };

        for (String route : routes) {
            mockMvc.perform(get(route))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        }
    }

    @Test
    void everyAuthenticatedRoleCanEnterUserLevelReadRoutes() throws Exception {
        org.mockito.Mockito.when(boothService.getBooths(null, null, null)).thenReturn(List.of());

        for (UserRole role : UserRole.values()) {
            mockMvc.perform(get("/api/booths")
                            .header("Authorization", "Bearer " + token(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void boothApplicationsArePermitAll() throws Exception {
        // No controller yet — 404 confirms the route is not blocked by security (permitAll works)
        mockMvc.perform(post("/api/booth-applications"))
                .andExpect(status().isNotFound());
    }

    @Test
    void boothManagerAndFestivalAdminRouteRejectsMissingAuthentication() throws Exception {
        mockMvc.perform(patch("/api/booths/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void regularUsersCannotEnterBoothManagerRoutes() throws Exception {
        mockMvc.perform(patch("/api/booths/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void regularUsersCannotEnterFestivalAdminRoutes() throws Exception {
        mockMvc.perform(patch("/api/festival")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(patch("/api/festival")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void boothManagersPassBoothManagerGate() throws Exception {
        mockMvc.perform(patch("/api/booths/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void festivalAdminsPassBothGates() throws Exception {
        mockMvc.perform(patch("/api/festival")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/api/booths/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void onlyUserRoleCanAccessGeneralUserWaitingRoutes() throws Exception {
        org.mockito.Mockito.when(waitingService.getMyWaitings(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/waitings")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/waitings")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/waitings")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isForbidden());
    }

    private String token(UserRole role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(role.name().toLowerCase() + "user")
                .claim("festivalId", UUID.randomUUID().toString())
                .claim("role", role.name())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
