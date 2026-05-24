package com.festi.backend.notification;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push-subscriptions")
@RequiredArgsConstructor
@Tag(name = "Push Subscriptions", description = "User Web Push subscription APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    @Operation(summary = "Register push subscription", description = "Registers or updates the authenticated user's Web Push subscription.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Push subscription registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "User was not found")
    })
    @PostMapping
    public ResponseEntity<PushSubscriptionDTO.Response> register(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PushSubscriptionDTO.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pushSubscriptionService.register(currentUser, request));
    }

    @Operation(summary = "Remove push subscription", description = "Removes one Web Push subscription owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Push subscription removed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "Push subscription was not found")
    })
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> remove(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Push subscription ID") @PathVariable UUID subscriptionId
    ) {
        pushSubscriptionService.remove(currentUser, subscriptionId);
        return ResponseEntity.noContent().build();
    }
}
