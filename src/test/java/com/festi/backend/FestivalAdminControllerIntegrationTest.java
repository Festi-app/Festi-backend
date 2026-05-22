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

import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.FestivalDTO;
import com.festi.backend.festival.FestivalDayDTO;
import com.festi.backend.festival.FestivalService;
import com.festi.backend.festival.NoticeDTO;
import com.festi.backend.festival.TimelineDTO;
import com.festi.backend.location.LocationDTO;
import com.festi.backend.location.LocationService;
import com.festi.backend.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
class FestivalAdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private FestivalService festivalService;

    @MockitoBean
    private LocationService locationService;

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
    void festivalAdminCanMutateFestivalResources() throws Exception {
        UUID festivalDayId = UUID.randomUUID();
        UUID noticeId = UUID.randomUUID();
        UUID timelineId = UUID.randomUUID();
        String token = token(UserRole.FESTIVAL_ADMIN);
        when(festivalService.updateFestival(any()))
                .thenReturn(new FestivalDTO.Response(festivalId, "Updated", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc"));
        when(festivalService.createFestivalDay(any()))
                .thenReturn(new FestivalDayDTO.Response(festivalDayId, LocalDate.of(2026, 5, 18),
                        LocalTime.of(10, 0), LocalTime.of(17, 0), LocalTime.of(18, 0), LocalTime.of(23, 0)));
        when(festivalService.updateFestivalDay(eq(festivalDayId), any()))
                .thenReturn(new FestivalDayDTO.Response(festivalDayId, LocalDate.of(2026, 5, 19),
                        LocalTime.of(10, 0), LocalTime.of(17, 0), LocalTime.of(18, 0), LocalTime.of(23, 0)));
        when(festivalService.createNotice(any()))
                .thenReturn(new NoticeDTO.Response(noticeId, "Notice", "Content", true, null));
        when(festivalService.updateNotice(eq(noticeId), any()))
                .thenReturn(new NoticeDTO.Response(noticeId, "Updated Notice", "Content", false, null));
        when(festivalService.createTimeline(any()))
                .thenReturn(new TimelineDTO.Response(timelineId, new FestivalDayDTO.Summary(festivalDayId, LocalDate.of(2026, 5, 18)),
                        "Stage", "Artist", LocalTime.of(18, 0), LocalTime.of(19, 0)));
        when(festivalService.updateTimeline(eq(timelineId), any()))
                .thenReturn(new TimelineDTO.Response(timelineId, new FestivalDayDTO.Summary(festivalDayId, LocalDate.of(2026, 5, 18)),
                        "Updated Stage", "Artist", LocalTime.of(18, 0), LocalTime.of(19, 0)));

        mockMvc.perform(patch("/api/festival")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FestivalDTO.UpdateRequest(
                                "Updated", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));

        mockMvc.perform(post("/api/festival/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dayRequest(LocalDate.of(2026, 5, 18)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.day").value("2026-05-18"));

        mockMvc.perform(patch("/api/festival/days/{festivalDayId}", festivalDayId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dayRequest(LocalDate.of(2026, 5, 19)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value("2026-05-19"));

        mockMvc.perform(delete("/api/festival/days/{festivalDayId}", festivalDayId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(festivalService).deleteFestivalDay(festivalDayId);

        mockMvc.perform(post("/api/festival/notices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoticeDTO.Request("Notice", "Content", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Notice"));

        mockMvc.perform(patch("/api/festival/notices/{noticeId}", noticeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoticeDTO.Request("Updated Notice", "Content", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));

        mockMvc.perform(delete("/api/festival/notices/{noticeId}", noticeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(festivalService).deleteNotice(noticeId);

        mockMvc.perform(post("/api/festival/timelines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timelineRequest(festivalDayId, "Stage"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Stage"));

        mockMvc.perform(patch("/api/festival/timelines/{timelineId}", timelineId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timelineRequest(festivalDayId, "Updated Stage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Stage"));

        mockMvc.perform(delete("/api/festival/timelines/{timelineId}", timelineId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(festivalService).deleteTimeline(timelineId);
    }

    @Test
    void festivalAdminCanCreateAndAssignLocationSlots() throws Exception {
        UUID dayId = UUID.randomUUID();
        UUID boothId = UUID.randomUUID();
        short locationId = 10;
        String token = token(UserRole.FESTIVAL_ADMIN);
        when(locationService.createSlots(any()))
                .thenReturn(List.of(new LocationDTO.Response(locationId, BoothType.NIGHT, (short) 1,
                        new FestivalDayDTO.Summary(dayId, LocalDate.of(2026, 5, 18)), "A", null)));
        when(locationService.assignBooth(eq(locationId), any()))
                .thenReturn(new LocationDTO.Response(locationId, BoothType.NIGHT, (short) 1,
                        new FestivalDayDTO.Summary(dayId, LocalDate.of(2026, 5, 18)), "A", null));

        mockMvc.perform(post("/api/locations/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocationDTO.SlotsRequest(
                                dayId, BoothType.NIGHT, List.of(new LocationDTO.ZoneSlotsRequest("A", (short) 2))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].zoneLabel").value("A"));

        mockMvc.perform(post("/api/locations/{locationId}/assignment", locationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocationDTO.AssignmentRequest(boothId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/locations/{locationId}/assignment", locationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(locationService).removeAssignment(locationId);
    }

    @Test
    void nonFestivalAdminsCannotEnterFestivalAdminRoutes() throws Exception {
        mockMvc.perform(post("/api/festival/days")
                        .header("Authorization", "Bearer " + token(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dayRequest(LocalDate.of(2026, 5, 18)))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/locations/slots")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocationDTO.SlotsRequest(
                                UUID.randomUUID(), BoothType.NIGHT, List.of(new LocationDTO.ZoneSlotsRequest("A", (short) 2))))))
                .andExpect(status().isForbidden());
    }

    private FestivalDayDTO.Request dayRequest(LocalDate day) {
        return new FestivalDayDTO.Request(
                day,
                LocalTime.of(10, 0),
                LocalTime.of(17, 0),
                LocalTime.of(18, 0),
                LocalTime.of(23, 0)
        );
    }

    private TimelineDTO.Request timelineRequest(UUID festivalDayId, String title) {
        return new TimelineDTO.Request(
                festivalDayId,
                title,
                "Artist",
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        );
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
