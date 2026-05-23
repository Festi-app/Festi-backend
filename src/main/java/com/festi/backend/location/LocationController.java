package com.festi.backend.location;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.booth.BoothType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "Create location slots", description = "Creates unassigned location slots from zone labels and counts.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Location slots created"),
            @ApiResponse(responseCode = "400", description = "Invalid slots request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Festival day was not found"),
            @ApiResponse(responseCode = "409", description = "Location slot already exists")
    })
    @PostMapping("/slots")
    public ResponseEntity<List<LocationDTO.Response>> createSlots(
            @Valid @RequestBody LocationDTO.SlotsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createSlots(request));
    }

    @Operation(summary = "Assign booth to location", description = "Assigns a booth to one unassigned location slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booth assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid assignment request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Location slot or booth was not found"),
            @ApiResponse(responseCode = "409", description = "Location slot is already assigned")
    })
    @PostMapping("/{locationId}/assignment")
    public ResponseEntity<LocationDTO.Response> assignBooth(
            @Parameter(description = "Location slot ID") @PathVariable short locationId,
            @Valid @RequestBody LocationDTO.AssignmentRequest request
    ) {
        return ResponseEntity.ok(locationService.assignBooth(locationId, request));
    }

    @Operation(summary = "Remove booth assignment", description = "Removes the booth assignment from one location slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booth assignment removed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Location slot was not found")
    })
    @DeleteMapping("/{locationId}/assignment")
    public ResponseEntity<Void> removeAssignment(
            @Parameter(description = "Location slot ID") @PathVariable short locationId
    ) {
        locationService.removeAssignment(locationId);
        return ResponseEntity.noContent().build();
    }
}
