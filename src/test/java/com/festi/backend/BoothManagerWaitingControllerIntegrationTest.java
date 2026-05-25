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

import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothType;
import com.festi.backend.user.UserRole;
import com.festi.backend.waiting.WaitingDTO;
import com.festi.backend.waiting.WaitingService;
import com.festi.backend.waiting.WaitingStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
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

@SpringBootTest
@ActiveProfiles("test")
class BoothManagerWaitingControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private WaitingService waitingService;

    private MockMvc mockMvc;

    private UUID festivalId;

    private UUID boothId;

    private UUID waitingId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        festivalId = UUID.randomUUID();
        boothId = UUID.randomUUID();
        waitingId = UUID.randomUUID();
    }

    @Test
    void boothManagerCanManageActiveWaitingsForAssignedBooth() throws Exception {
        when(waitingService.getActiveWaitings(any(), eq(boothId))).thenReturn(List.of(waiting(WaitingStatus.WAITING)));
        when(waitingService.callWaiting(any(), eq(waitingId))).thenReturn(waiting(WaitingStatus.CALLED));
        when(waitingService.updateWaitingStatus(any(), eq(waitingId), any())).thenReturn(waiting(WaitingStatus.SEATED));
        when(waitingService.updateWaitingOpenStatus(any(), eq(boothId), any())).thenReturn(booth(true));
        String token = token(UserRole.BOOTH_MANAGER);

        mockMvc.perform(get("/api/booths/{boothId}/waitings", boothId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("WAITING"));

        mockMvc.perform(post("/api/waitings/{waitingId}/call", waitingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"));

        mockMvc.perform(patch("/api/waitings/{waitingId}/status", waitingId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SEATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEATED"));

        mockMvc.perform(patch("/api/booths/{boothId}/waitings/status", boothId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"open\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isWaitingOpen").value(true));
    }

    @Test
    void festivalAdminCanEnterWaitingManagementRoutesAndUserCannot() throws Exception {
        when(waitingService.getActiveWaitings(any(), eq(boothId))).thenReturn(List.of());

        mockMvc.perform(get("/api/booths/{boothId}/waitings", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/booths/{boothId}/waitings", boothId)
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void openApiContainsWaitingManagementPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/booths/{boothId}/waitings']").exists())
                .andExpect(jsonPath("$.paths['/api/waitings/{waitingId}/call']").exists())
                .andExpect(jsonPath("$.paths['/api/waitings/{waitingId}/status']").exists())
                .andExpect(jsonPath("$.paths['/api/booths/{boothId}/waitings/status']").exists());
    }

    private WaitingDTO.Response waiting(WaitingStatus status) {
        return new WaitingDTO.Response(
                waitingId,
                new BoothDTO.Summary(boothId, "Night booth", BoothCategory.ALCOHOL, BoothType.NIGHT, "desc", null, true, 1),
                (short) 2,
                status,
                status == WaitingStatus.WAITING ? (short) 0 : (short) 1,
                OffsetDateTime.parse("2026-05-25T00:00:00Z"),
                status == WaitingStatus.WAITING ? 1 : null,
                status == WaitingStatus.CALLED ? 1 : null
        );
    }

    private BoothDTO.Detail booth(boolean waitingOpen) {
        return new BoothDTO.Detail(
                boothId, "Night booth", BoothCategory.ALCOHOL, BoothType.NIGHT,
                "desc", "18:00-23:00", null, waitingOpen);
    }

    private String token(UserRole role) {
        String subject = role == UserRole.BOOTH_MANAGER ? "manager1" : role.name().toLowerCase() + "user";
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .claim("festivalId", festivalId.toString())
                .claim("role", role.name())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
