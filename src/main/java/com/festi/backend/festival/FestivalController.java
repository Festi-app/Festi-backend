package com.festi.backend.festival;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festival")
@RequiredArgsConstructor
@Tag(name = "Festival", description = "Festival information APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class FestivalController {

    private final FestivalService festivalService;

    @Operation(summary = "Get festival", description = "Returns basic information for the active festival.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Festival retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Festival was not found")
    })
    @GetMapping
    public ResponseEntity<FestivalDTO.Response> getFestival() {
        return ResponseEntity.ok(festivalService.getFestival());
    }

    @Operation(summary = "List festival notices", description = "Returns notices for the active festival.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notice list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required")
    })
    @GetMapping("/notices")
    public ResponseEntity<List<NoticeDTO.Response>> getNotices() {
        return ResponseEntity.ok(festivalService.getNotices());
    }

    @Operation(summary = "List festival timelines", description = "Returns timeline entries for the active festival.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timeline list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required")
    })
    @GetMapping("/timelines")
    public ResponseEntity<List<TimelineDTO.Response>> getTimelines() {
        return ResponseEntity.ok(festivalService.getTimelines());
    }
}
