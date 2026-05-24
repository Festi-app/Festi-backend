package com.festi.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothService;
import com.festi.backend.booth.BoothType;
import com.festi.backend.menu.MenuDTO;
import com.festi.backend.menu.MenuService;
import com.festi.backend.user.UserRole;
import com.festi.backend.waiting.WaitingService;
import java.time.Instant;
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
class BoothManagerControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private BoothService boothService;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private WaitingService waitingService;

    private MockMvc mockMvc;

    private UUID festivalId;
    private UUID boothId;
    private UUID menuId;

    private BoothDTO.Detail boothDetail;
    private MenuDTO.Response menuResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        festivalId = UUID.randomUUID();
        boothId = UUID.randomUUID();
        menuId = UUID.randomUUID();
        boothDetail = new BoothDTO.Detail(boothId, "부스명", BoothCategory.ALCOHOL,
                BoothType.NIGHT, "설명", "18:00~23:00", "image.jpg", false);
        menuResponse = new MenuDTO.Response(menuId, "메뉴명", 5000, "설명", "image.jpg", false, (short) 1);
    }

    // ── PATCH /api/booths/{boothId} ──────────────────────────────────────────

    @Test
    void boothManagerCanUpdateBooth() throws Exception {
        when(boothService.updateBooth(any(), eq(boothId), any())).thenReturn(boothDetail);

        mockMvc.perform(patch("/api/booths/{boothId}", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoothRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("부스명"));
    }

    @Test
    void festivalAdminCanUpdateBooth() throws Exception {
        when(boothService.updateBooth(any(), eq(boothId), any())).thenReturn(boothDetail);

        mockMvc.perform(patch("/api/booths/{boothId}", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoothRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotUpdateBooth() throws Exception {
        mockMvc.perform(patch("/api/booths/{boothId}", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoothRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedCannotUpdateBooth() throws Exception {
        mockMvc.perform(patch("/api/booths/{boothId}", boothId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoothRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/booths/{boothId}/menus ─────────────────────────────────────

    @Test
    void boothManagerCanCreateMenu() throws Exception {
        when(menuService.createMenu(any(), eq(boothId), any())).thenReturn(menuResponse);

        mockMvc.perform(post("/api/booths/{boothId}/menus", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("메뉴명"))
                .andExpect(jsonPath("$.price").value(5000));
    }

    @Test
    void festivalAdminCanCreateMenu() throws Exception {
        when(menuService.createMenu(any(), eq(boothId), any())).thenReturn(menuResponse);

        mockMvc.perform(post("/api/booths/{boothId}/menus", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void userCannotCreateMenu() throws Exception {
        mockMvc.perform(post("/api/booths/{boothId}/menus", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuRequest())))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/booths/{boothId}/menus/{menuId} ───────────────────────────

    @Test
    void boothManagerCanUpdateMenu() throws Exception {
        when(menuService.updateMenu(any(), eq(boothId), eq(menuId), any())).thenReturn(menuResponse);

        mockMvc.perform(patch("/api/booths/{boothId}/menus/{menuId}", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("메뉴명"));
    }

    @Test
    void userCannotUpdateMenu() throws Exception {
        mockMvc.perform(patch("/api/booths/{boothId}/menus/{menuId}", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuRequest())))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/booths/{boothId}/menus/{menuId} ──────────────────────────

    @Test
    void boothManagerCanDeleteMenu() throws Exception {
        mockMvc.perform(delete("/api/booths/{boothId}/menus/{menuId}", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isNoContent());

        verify(menuService).deleteMenu(any(), eq(boothId), eq(menuId));
    }

    @Test
    void festivalAdminCanDeleteMenu() throws Exception {
        mockMvc.perform(delete("/api/booths/{boothId}/menus/{menuId}", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void userCannotDeleteMenu() throws Exception {
        mockMvc.perform(delete("/api/booths/{boothId}/menus/{menuId}", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/booths/{boothId}/menus/{menuId}/sold-out ───────────────────

    @Test
    void boothManagerCanMarkSoldOut() throws Exception {
        MenuDTO.Response soldOut = new MenuDTO.Response(menuId, "메뉴명", 5000, "설명", "image.jpg", true, (short) 1);
        when(menuService.markSoldOut(any(), eq(boothId), eq(menuId))).thenReturn(soldOut);

        mockMvc.perform(post("/api/booths/{boothId}/menus/{menuId}/sold-out", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSoldOut").value(true));
    }

    @Test
    void userCannotMarkSoldOut() throws Exception {
        mockMvc.perform(post("/api/booths/{boothId}/menus/{menuId}/sold-out", boothId, menuId)
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/booths/food-trucks ─────────────────────────────────────────

    @Test
    void festivalAdminCanCreateFoodTruck() throws Exception {
        BoothDTO.Detail foodTruck = new BoothDTO.Detail(UUID.randomUUID(), "푸드트럭A",
                BoothCategory.ACTIVITY, BoothType.FOOD_TRUCK, "설명", "11:00~20:00", null, false);
        when(boothService.createFoodTruck(any())).thenReturn(foodTruck);

        mockMvc.perform(post("/api/booths/food-trucks")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BoothDTO.CreateFoodTruckRequest("푸드트럭A", null, "설명", "11:00~20:00", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FOOD_TRUCK"));
    }

    @Test
    void boothManagerCannotCreateFoodTruck() throws Exception {
        mockMvc.perform(post("/api/booths/food-trucks")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BoothDTO.CreateFoodTruckRequest("푸드트럭B", null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCreateFoodTruck() throws Exception {
        mockMvc.perform(post("/api/booths/food-trucks")
                        .header("Authorization", "Bearer " + token(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BoothDTO.CreateFoodTruckRequest("푸드트럭C", null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BoothDTO.UpdateRequest updateBoothRequest() {
        return new BoothDTO.UpdateRequest("부스명", BoothCategory.ALCOHOL, "설명", "18:00~23:00", "image.jpg");
    }

    private MenuDTO.Request menuRequest() {
        return new MenuDTO.Request("메뉴명", 5000, "설명", "image.jpg", (short) 1);
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
