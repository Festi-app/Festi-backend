package com.festi.backend.location;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.booth.BoothType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Festival booth location APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "List booth locations", description = "Returns booth locations for a festival day and booth type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location list retrieved"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid query parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Festival or festival day was not found")
    })
    @GetMapping
    public ResponseEntity<List<LocationDTO.Response>> getLocations(
            @Parameter(description = "Festival day in ISO date format, for example 2026-05-22", required = true)
            @RequestParam(required = false)
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate day,
            @Parameter(description = "Booth type", required = true)
            @RequestParam(required = false) @NotNull BoothType type
    ) {
        return ResponseEntity.ok(locationService.getLocations(day, type));
    }
}
