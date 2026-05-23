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

import com.festi.backend.booth.BoothApplicationDTO;
import com.festi.backend.booth.BoothApplicationService;
import com.festi.backend.booth.BoothApplicationStatus;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.user.UserRole;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class BoothApplicationControllerIntegrationTest {

    private static final String BOOTH_MANAGER_ID = "manager1";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private BoothApplicationService boothApplicationService;

    private MockMvc mockMvc;

    private UUID festivalId;

    private UUID applicationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        festivalId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
    }

    @Test
    void createApplicationIsPublicAndReturnsApplicationResponse() throws Exception {
        when(boothApplicationService.createApplication(any()))
                .thenReturn(response(BoothApplicationStatus.PENDING, null));

        mockMvc.perform(post("/api/booth-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId").value("manager1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void meEndpointRequiresBoothManagerOrFestivalAdmin() throws Exception {
        when(boothApplicationService.getMyApplication(eq(BOOTH_MANAGER_ID), eq(festivalId)))
                .thenReturn(response(BoothApplicationStatus.PENDING, null));

        mockMvc.perform(get("/api/booth-applications/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/booth-applications/me")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/booth-applications/me")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicantId").value(BOOTH_MANAGER_ID));

        verify(boothApplicationService).getMyApplication(eq(BOOTH_MANAGER_ID), eq(festivalId));
    }

    @Test
    void adminEndpointsRequireFestivalAdmin() throws Exception {
        when(boothApplicationService.getApplications(any())).thenReturn(List.of(response(BoothApplicationStatus.PENDING, null)));

        mockMvc.perform(get("/api/admin/booth-applications")
                        .header("Authorization", "Bearer " + token(UserRole.USER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/booth-applications")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/booth-applications")
                        .header("Authorization", "Bearer " + token(UserRole.FESTIVAL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void adminCanReviewAndDeleteApplications() throws Exception {
        when(boothApplicationService.getApplication(any(), eq(applicationId)))
                .thenReturn(response(BoothApplicationStatus.PENDING, null));
        when(boothApplicationService.approveApplication(any(), eq(applicationId)))
                .thenReturn(response(BoothApplicationStatus.APPROVED, null));
        when(boothApplicationService.rejectApplication(any(), eq(applicationId), eq("not enough detail")))
                .thenReturn(response(BoothApplicationStatus.REJECTED, "not enough detail"));

        String token = token(UserRole.FESTIVAL_ADMIN);

        mockMvc.perform(get("/api/admin/booth-applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()));

        mockMvc.perform(post("/api/admin/booth-applications/{applicationId}/approve", applicationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/admin/booth-applications/{applicationId}/reject", applicationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewMemo":"not enough detail"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewMemo").value("not enough detail"));

        mockMvc.perform(delete("/api/admin/booth-applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(boothApplicationService).deleteApplication(any(), eq(applicationId));
    }

    @Test
    void openApiContainsBoothApplicationPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/booth-applications']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/booth-applications']").exists());
    }

    private BoothApplicationDTO.CreateRequest createRequest() {
        return new BoothApplicationDTO.CreateRequest(
                BOOTH_MANAGER_ID,
                "Password1!",
                "Manager",
                "01012345678",
                "Night Booth",
                BoothType.NIGHT,
                BoothCategory.ALCOHOL,
                "https://example.com/booth.png",
                "description"
        );
    }

    private BoothApplicationDTO.Response response(BoothApplicationStatus status, String reviewMemo) {
        return new BoothApplicationDTO.Response(
                applicationId,
                festivalId,
                BOOTH_MANAGER_ID,
                "Night Booth",
                BoothType.NIGHT,
                BoothCategory.ALCOHOL,
                "https://example.com/booth.png",
                "description",
                status,
                reviewMemo,
                OffsetDateTime.parse("2026-05-23T00:00:00Z"),
                OffsetDateTime.parse("2026-05-23T00:00:00Z")
        );
    }

    private String token(UserRole role) {
        String subject = role == UserRole.BOOTH_MANAGER ? BOOTH_MANAGER_ID : role.name().toLowerCase() + "user";
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
