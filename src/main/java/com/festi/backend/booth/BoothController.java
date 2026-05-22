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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "Register waiting", description = "Registers the authenticated user for a waiting slot at the specified booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Waiting registered"),
            @ApiResponse(responseCode = "400", description = "Booth is not a NIGHT booth, waiting is closed, or user has reached the maximum of 3 active waitings"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @PostMapping("/{boothId}/waitings")
    public ResponseEntity<WaitingDTO.Response> registerWaiting(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody WaitingDTO.Request request
    ) {
        WaitingDTO.Response response = waitingService.registerWaiting(
                currentUser.id(), currentUser.festivalId(), boothId, request.partySize());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
