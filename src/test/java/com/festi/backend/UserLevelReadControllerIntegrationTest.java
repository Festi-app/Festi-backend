package com.festi.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothService;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.FestivalDTO;
import com.festi.backend.festival.FestivalService;
import com.festi.backend.festival.NoticeDTO;
import com.festi.backend.location.LocationDTO;
import com.festi.backend.location.LocationService;
import com.festi.backend.menu.MenuDTO;
import com.festi.backend.menu.MenuService;
import com.festi.backend.user.UserRole;
import com.festi.backend.waiting.WaitingDTO;
import com.festi.backend.waiting.WaitingService;
import com.festi.backend.waiting.WaitingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
class UserLevelReadControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private BoothService boothService;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private FestivalService festivalService;

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
    void authenticatedUsersCanReadAllUserLevelEndpoints() throws Exception {
        UUID boothId = UUID.randomUUID();
        UUID waitingId = UUID.randomUUID();
        BoothDTO.Summary boothSummary = new BoothDTO.Summary(
                boothId, "booth", BoothCategory.INFO, BoothType.DAY, "image", false);
        when(boothService.getBooths(any(), any(), any())).thenReturn(List.of(boothSummary));
        when(boothService.getBooth(boothId)).thenReturn(new BoothDTO.Detail(
                boothId, "booth", BoothCategory.INFO, BoothType.DAY, "desc", "09:00-18:00", "image", false));
        when(menuService.getMenus(boothId)).thenReturn(List.of(
                new MenuDTO.Response(UUID.randomUUID(), "menu", 5000, "desc", "image", false, (short) 1)));
        when(locationService.getLocations(LocalDate.of(2026, 5, 20), BoothType.DAY)).thenReturn(List.of(
                new LocationDTO.Response((short) 1, BoothType.DAY, (short) 1, LocalDate.of(2026, 5, 20), "A", boothSummary)));
        when(festivalService.getFestival()).thenReturn(new FestivalDTO.Response(
                UUID.randomUUID(), "Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc"));
        when(festivalService.getNotices()).thenReturn(List.of(
                new NoticeDTO.Response(UUID.randomUUID(), "notice", "content", OffsetDateTime.of(
                        2026, 5, 18, 10, 0, 0, 0, ZoneOffset.UTC))));
        when(waitingService.getMyWaitings(any())).thenReturn(List.of(
                new WaitingDTO.Response(waitingId, boothSummary, (short) 2, WaitingStatus.WAITING, (short) 0,
                        OffsetDateTime.of(2026, 5, 18, 10, 0, 0, 0, ZoneOffset.UTC))));

        String token = token(UserRole.USER);
        mockMvc.perform(get("/api/booths").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("booth"));
        mockMvc.perform(get("/api/booths/" + boothId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("desc"));
        mockMvc.perform(get("/api/booths/" + boothId + "/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("menu"));
        mockMvc.perform(get("/api/locations")
                        .param("day", "2026-05-20")
                        .param("type", "DAY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].boothSummary.name").value("booth"));
        mockMvc.perform(get("/api/festival").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Festi"));
        mockMvc.perform(get("/api/festival/notices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("notice"));
        mockMvc.perform(get("/api/waitings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].boothSummary.name").value("booth"));
    }

    @Test
    void locationsRequireDayAndTypeFilters() throws Exception {
        mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isBadRequest());
    }

    private String token(UserRole role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", role.name().toLowerCase() + "@example.com")
                .claim("role", role.name())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
