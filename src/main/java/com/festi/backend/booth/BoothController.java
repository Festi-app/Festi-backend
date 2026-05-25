package com.festi.backend.booth;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.waiting.WaitingDTO;
import com.festi.backend.waiting.WaitingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
@Tag(name = "Booths", description = "Booth discovery and waiting APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class BoothController {

    private final BoothService boothService;
    private final WaitingService waitingService;

    @Operation(summary = "List booths", description = "Returns booth summaries, optionally filtered by day, booth type, and category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth list retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Festival or festival day was not found")
    })
    @GetMapping
    public ResponseEntity<List<BoothDTO.Summary>> getBooths(
            @Parameter(description = "Festival day filter in ISO date format, for example 2026-05-22")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate day,
            @Parameter(description = "Booth type filter")
            @RequestParam(required = false)
            BoothType type,
            @Parameter(description = "Booth category filter")
            @RequestParam(required = false)
            BoothCategory category
    ) {
        return ResponseEntity.ok(boothService.getBooths(day, type, category));
    }

    @Operation(summary = "Get booth detail", description = "Returns detailed information for a booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth detail retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid booth ID"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @GetMapping("/{boothId}")
    public ResponseEntity<BoothDTO.Detail> getBooth(
            @Parameter(description = "Booth ID")
            @PathVariable UUID boothId
    ) {
        return ResponseEntity.ok(boothService.getBooth(boothId));
    }

    @Operation(summary = "List active booth waitings", description = "Returns active waiting registrations for a managed booth in registration order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active waiting list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @GetMapping("/{boothId}/waitings")
    public ResponseEntity<List<WaitingDTO.Response>> getActiveWaitings(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(waitingService.getActiveWaitings(currentUser, boothId));
    }
    @Operation(summary = "Create food truck", description = "Creates a food truck booth. Only a festival admin can call this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Food truck created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required")
    })
    @PostMapping("/admin/food-trucks")
    public ResponseEntity<BoothDTO.Detail> createFoodTruck(
            @Valid @RequestBody BoothDTO.CreateFoodTruckRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boothService.createFoodTruck(request));
    }

    @Operation(summary = "Update food truck", description = "Updates food truck information. Only a festival admin can call this endpoint. All fields are optional — only provided fields are updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Food truck updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or booth is not a food truck"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @PatchMapping("/admin/food-trucks/{boothId}")
    public ResponseEntity<BoothDTO.Detail> updateFoodTruck(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Valid @RequestBody BoothDTO.UpdateFoodTruckRequest request
    ) {
        return ResponseEntity.ok(boothService.updateFoodTruck(boothId, request));
    }

    @Operation(summary = "Delete food truck", description = "Deletes a food truck booth. Only a festival admin can call this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Food truck deleted"),
            @ApiResponse(responseCode = "400", description = "Booth is not a food truck"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @DeleteMapping("/admin/food-trucks/{boothId}")
    public ResponseEntity<Void> deleteFoodTruck(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId
    ) {
        boothService.deleteFoodTruck(boothId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update booth", description = "Updates booth information. Only the assigned booth manager or a festival admin can update a booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @PatchMapping("/{boothId}")
    public ResponseEntity<BoothDTO.Detail> updateBooth(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody BoothDTO.UpdateRequest request
    ) {
        return ResponseEntity.ok(boothService.updateBooth(currentUser, boothId, request));
    }

    @Operation(summary = "Upload booth image", description = "Replaces the managed image for a booth with a JPEG or PNG file within the configured upload limits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth image updated"),
            @ApiResponse(responseCode = "400", description = "Image file is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth was not found"),
            @ApiResponse(responseCode = "413", description = "Image file exceeds the configured size limit")
    })
    @PutMapping(value = "/{boothId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoothDTO.Detail> updateImage(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(boothService.updateImage(currentUser, boothId, image));
    }

    @Operation(summary = "Remove booth image", description = "Removes the managed booth image. The operation is idempotent when no image is set.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booth image removed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @DeleteMapping("/{boothId}/image")
    public ResponseEntity<Void> removeImage(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        boothService.removeImage(currentUser, boothId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register waiting", description = "Registers the authenticated user for a waiting slot at the specified booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Waiting registered"),
            @ApiResponse(responseCode = "400", description = "Request body is invalid, booth is not a NIGHT booth, waiting is closed, or user has reached the maximum of 3 active waitings"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found"),
            @ApiResponse(responseCode = "409", description = "An active waiting already exists for this booth")
    })
    @PostMapping("/{boothId}/waitings")
    public ResponseEntity<WaitingDTO.Response> registerWaiting(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody WaitingDTO.Request request
    ) {
        WaitingDTO.Response response = waitingService.registerWaiting(
                currentUser.id(), currentUser.festivalId(), boothId, request.partySize());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Open or close booth waiting", description = "Enables or disables waiting registration for a managed NIGHT booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waiting registration availability updated"),
            @ApiResponse(responseCode = "400", description = "Request body is invalid or booth is not a NIGHT booth"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @PatchMapping("/{boothId}/waitings/status")
    public ResponseEntity<BoothDTO.Detail> updateWaitingOpenStatus(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody WaitingDTO.OpenStatusRequest request
    ) {
        return ResponseEntity.ok(waitingService.updateWaitingOpenStatus(currentUser, boothId, request));
    }
}
