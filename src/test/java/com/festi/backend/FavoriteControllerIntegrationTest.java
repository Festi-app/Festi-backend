package com.festi.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothType;
import com.festi.backend.favorite.FavoriteDTO;
import com.festi.backend.favorite.FavoriteService;
import com.festi.backend.user.UserRole;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class FavoriteControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private FavoriteService favoriteService;

    private MockMvc mockMvc;

    private UUID festivalId;
    private UUID boothId;
    private UUID favoriteId;
    private BoothDTO.Summary boothSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        festivalId = UUID.randomUUID();
        boothId = UUID.randomUUID();
        favoriteId = UUID.randomUUID();
        boothSummary = new BoothDTO.Summary(boothId, "맥주부스", BoothCategory.ALCOHOL, BoothType.NIGHT,
                "설명", "image.jpg", false);
    }

    // ── GET /api/favorites ───────────────────────────────────────────────────

    @Test
    void userCanGetFavoriteList() throws Exception {
        FavoriteDTO.Response response = new FavoriteDTO.Response(
                favoriteId, boothSummary,
                OffsetDateTime.of(2026, 5, 18, 10, 0, 0, 0, ZoneOffset.UTC));
        when(favoriteService.getFavorites(any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(favoriteId.toString()))
                .andExpect(jsonPath("$[0].boothSummary.name").value("맥주부스"));
    }

    @Test
    void userCanGetEmptyFavoriteList() throws Exception {
        when(favoriteService.getFavorites(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void boothManagerCannotGetFavorites() throws Exception {
        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void festivalAdminCannotGetFavorites() throws Exception {
        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedCannotGetFavorites() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    // ── POST /api/favorites ──────────────────────────────────────────────────

    @Test
    void userCanAddFavorite() throws Exception {
        FavoriteDTO.Response response = new FavoriteDTO.Response(
                favoriteId, boothSummary,
                OffsetDateTime.of(2026, 5, 18, 10, 0, 0, 0, ZoneOffset.UTC));
        when(favoriteService.addFavorite(any(), any(), eq(boothId))).thenReturn(response);

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FavoriteDTO.Request(boothId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(favoriteId.toString()))
                .andExpect(jsonPath("$.boothSummary.name").value("맥주부스"));
    }

    @Test
    void boothManagerCannotAddFavorite() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FavoriteDTO.Request(boothId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAddFavorite() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FavoriteDTO.Request(boothId))))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/favorites/{favoriteId} ───────────────────────────────────

    @Test
    void userCanRemoveFavorite() throws Exception {
        mockMvc.perform(delete("/api/favorites/{favoriteId}", favoriteId)
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFavorite(any(), eq(favoriteId));
    }

    @Test
    void boothManagerCannotRemoveFavorite() throws Exception {
        mockMvc.perform(delete("/api/favorites/{favoriteId}", favoriteId)
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotRemoveFavorite() throws Exception {
        mockMvc.perform(delete("/api/favorites/{favoriteId}", favoriteId))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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
