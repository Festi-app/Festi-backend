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

import com.festi.backend.notification.PushSubscriptionDTO;
import com.festi.backend.notification.PushSubscriptionService;
import com.festi.backend.user.UserRole;
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

@SpringBootTest
@ActiveProfiles("test")
class PushSubscriptionControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private PushSubscriptionService pushSubscriptionService;

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
    void userCanRegisterAndRemovePushSubscription() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        when(pushSubscriptionService.register(any(), any()))
                .thenReturn(new PushSubscriptionDTO.Response(subscriptionId, "https://push.example.com/subscription/1"));
        String token = token(UserRole.USER);

        mockMvc.perform(post("/api/push-subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "https://push.example.com/subscription/1",
                                  "keys": {"p256dh": "key", "auth": "auth"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(subscriptionId.toString()));

        mockMvc.perform(delete("/api/push-subscriptions/{subscriptionId}", subscriptionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(pushSubscriptionService).remove(any(), eq(subscriptionId));
    }

    @Test
    void nonUserRolesCannotManagePushSubscriptions() throws Exception {
        mockMvc.perform(post("/api/push-subscriptions")
                        .header("Authorization", "Bearer " + token(UserRole.BOOTH_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "https://push.example.com/subscription/1",
                                  "keys": {"p256dh": "key", "auth": "auth"}
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void openApiContainsPushSubscriptionPath() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/push-subscriptions']").exists())
                .andExpect(jsonPath("$.paths['/api/push-subscriptions/{subscriptionId}']").exists());
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
