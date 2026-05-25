package com.festi.backend.waiting;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waitings")
@RequiredArgsConstructor
@Tag(name = "Waitings", description = "User waiting status APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class WaitingController {

    private final WaitingService waitingService;

    @Operation(summary = "List my waitings", description = "Returns the authenticated user's waiting registrations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waiting list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required")
    })
    @GetMapping
    public ResponseEntity<List<WaitingDTO.Response>> getMyWaitings(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(waitingService.getMyWaitings(currentUser.id(), currentUser.festivalId()));
    }

    @Operation(summary = "Cancel waiting", description = "Cancels the authenticated user's waiting registration.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Waiting cancelled"),
            @ApiResponse(responseCode = "400", description = "Waiting cannot be cancelled in its current status"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "Waiting was not found")
    })
    @DeleteMapping("/{waitingId}")
    public ResponseEntity<Void> cancelWaiting(
            @Parameter(description = "Waiting ID") @PathVariable UUID waitingId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        waitingService.cancelWaiting(currentUser.id(), currentUser.festivalId(), waitingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Call waiting customer", description = "Calls an active waiting registration. Recalling a called waiting increments its call count.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waiting called"),
            @ApiResponse(responseCode = "400", description = "Waiting is no longer active"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Waiting was not found")
    })
    @PostMapping("/{waitingId}/call")
    public ResponseEntity<WaitingDTO.Response> callWaiting(
            @Parameter(description = "Waiting ID") @PathVariable UUID waitingId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(waitingService.callWaiting(currentUser, waitingId));
    }

    @Operation(summary = "Update waiting status", description = "Marks a called waiting registration as seated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waiting status updated"),
            @ApiResponse(responseCode = "400", description = "Request body or status transition is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Waiting was not found")
    })
    @PatchMapping("/{waitingId}/status")
    public ResponseEntity<WaitingDTO.Response> updateWaitingStatus(
            @Parameter(description = "Waiting ID") @PathVariable UUID waitingId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody WaitingDTO.StatusRequest request
    ) {
        return ResponseEntity.ok(waitingService.updateWaitingStatus(currentUser, waitingId, request));
    }
}
