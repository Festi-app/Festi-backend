package com.festi.backend.booth;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
@Tag(name = "Booths", description = "Booth discovery APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class BoothController {

    private final BoothService boothService;

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
}
