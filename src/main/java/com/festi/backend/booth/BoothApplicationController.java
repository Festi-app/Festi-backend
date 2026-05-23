package com.festi.backend.booth;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Booth Applications", description = "Booth manager application and review APIs")
public class BoothApplicationController {

    private final BoothApplicationService boothApplicationService;

    @Operation(summary = "Create booth application", description = "Creates a BOOTH_MANAGER account and a pending booth application.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booth application created"),
            @ApiResponse(responseCode = "400", description = "Invalid application request"),
            @ApiResponse(responseCode = "404", description = "Festival was not found"),
            @ApiResponse(responseCode = "409", description = "ID is already in use")
    })
    @PostMapping("/api/booth-applications")
    public ResponseEntity<BoothApplicationDTO.Response> createApplication(
            @Valid @RequestBody BoothApplicationDTO.CreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(boothApplicationService.createApplication(request));
    }

    @Operation(summary = "Get my booth application", description = "Returns the authenticated booth manager's application.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth application retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth application was not found")
    })
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/api/booth-applications/me")
    public ResponseEntity<BoothApplicationDTO.Response> getMyApplication(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(boothApplicationService.getMyApplication(currentUser.id(), currentUser.festivalId()));
    }

    @Operation(summary = "List booth applications", description = "Returns booth applications for the active festival.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth application list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required")
    })
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/api/admin/booth-applications")
    public ResponseEntity<List<BoothApplicationDTO.Response>> getApplications(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(boothApplicationService.getApplications(currentUser.festivalId()));
    }

    @Operation(summary = "Get booth application", description = "Returns one booth application by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth application retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth application was not found")
    })
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/api/admin/booth-applications/{applicationId}")
    public ResponseEntity<BoothApplicationDTO.Response> getApplication(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Booth application ID") @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(boothApplicationService.getApplication(currentUser.festivalId(), applicationId));
    }

    @Operation(summary = "Approve booth application", description = "Approves a pending application and creates its booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth application approved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth application or manager was not found"),
            @ApiResponse(responseCode = "409", description = "Booth application was already reviewed")
    })
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/api/admin/booth-applications/{applicationId}/approve")
    public ResponseEntity<BoothApplicationDTO.Response> approveApplication(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Booth application ID") @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(boothApplicationService.approveApplication(currentUser.festivalId(), applicationId));
    }

    @Operation(summary = "Reject booth application", description = "Rejects a pending application with an optional review memo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth application rejected"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth application was not found"),
            @ApiResponse(responseCode = "409", description = "Booth application was already reviewed")
    })
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/api/admin/booth-applications/{applicationId}/reject")
    public ResponseEntity<BoothApplicationDTO.Response> rejectApplication(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Booth application ID") @PathVariable UUID applicationId,
            @RequestBody(required = false) BoothApplicationDTO.RejectRequest request
    ) {
        String reviewMemo = request == null ? null : request.reviewMemo();
        return ResponseEntity.ok(boothApplicationService.rejectApplication(currentUser.festivalId(), applicationId, reviewMemo));
    }

    @Operation(summary = "Delete booth application", description = "Deletes an unapproved application and its generated BOOTH_MANAGER account.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booth application deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth application was not found"),
            @ApiResponse(responseCode = "409", description = "Approved booth applications cannot be deleted")
    })
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/api/admin/booth-applications/{applicationId}")
    public ResponseEntity<Void> deleteApplication(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Booth application ID") @PathVariable UUID applicationId
    ) {
        boothApplicationService.deleteApplication(currentUser.festivalId(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
