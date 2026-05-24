package com.festi.backend.festival;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "Get festival days", description = "Returns the list of festival days with their dates and IDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Festival period retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Festival was not found")
    })
    @GetMapping("/days")
    public ResponseEntity<List<FestivalDayDTO.Summary>> getPeriod() {
        return ResponseEntity.ok(festivalService.getPeriod());
    }

    @Operation(summary = "Update festival", description = "Updates basic festival information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Festival updated"),
            @ApiResponse(responseCode = "400", description = "Invalid festival request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Festival was not found")
    })
    @PatchMapping
    public ResponseEntity<FestivalDTO.Response> updateFestival(
            @Valid @RequestBody FestivalDTO.UpdateRequest request
    ) {
        return ResponseEntity.ok(festivalService.updateFestival(request));
    }

    @Operation(summary = "Create festival day", description = "Creates an operating day and its daytime/nighttime hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Festival day created"),
            @ApiResponse(responseCode = "400", description = "Invalid festival day request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "409", description = "Festival day already exists")
    })
    @PostMapping("/days")
    public ResponseEntity<FestivalDayDTO.Response> createFestivalDay(
            @Valid @RequestBody FestivalDayDTO.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(festivalService.createFestivalDay(request));
    }

    @Operation(summary = "Update festival day", description = "Updates an operating day and its daytime/nighttime hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Festival day updated"),
            @ApiResponse(responseCode = "400", description = "Invalid festival day request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Festival day was not found"),
            @ApiResponse(responseCode = "409", description = "Festival day already exists")
    })
    @PatchMapping("/days/{festivalDayId}")
    public ResponseEntity<FestivalDayDTO.Response> updateFestivalDay(
            @PathVariable UUID festivalDayId,
            @Valid @RequestBody FestivalDayDTO.Request request
    ) {
        return ResponseEntity.ok(festivalService.updateFestivalDay(festivalDayId, request));
    }

    @Operation(summary = "Delete festival day", description = "Deletes one operating day.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Festival day deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Festival day was not found")
    })
    @DeleteMapping("/days/{festivalDayId}")
    public ResponseEntity<Void> deleteFestivalDay(@PathVariable UUID festivalDayId) {
        festivalService.deleteFestivalDay(festivalDayId);
        return ResponseEntity.noContent().build();
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

    @Operation(summary = "Create notice", description = "Creates a festival notice.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notice created"),
            @ApiResponse(responseCode = "400", description = "Invalid notice request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required")
    })
    @PostMapping("/notices")
    public ResponseEntity<NoticeDTO.Response> createNotice(
            @Valid @RequestBody NoticeDTO.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(festivalService.createNotice(request));
    }

    @Operation(summary = "Update notice", description = "Updates a festival notice.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notice updated"),
            @ApiResponse(responseCode = "400", description = "Invalid notice request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Notice was not found")
    })
    @PatchMapping("/notices/{noticeId}")
    public ResponseEntity<NoticeDTO.Response> updateNotice(
            @PathVariable UUID noticeId,
            @Valid @RequestBody NoticeDTO.Request request
    ) {
        return ResponseEntity.ok(festivalService.updateNotice(noticeId, request));
    }

    @Operation(summary = "Delete notice", description = "Deletes a festival notice.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notice deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Notice was not found")
    })
    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable UUID noticeId) {
        festivalService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
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

    @Operation(summary = "Create timeline", description = "Creates a festival timeline entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Timeline created"),
            @ApiResponse(responseCode = "400", description = "Invalid timeline request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Festival day was not found")
    })
    @PostMapping("/timelines")
    public ResponseEntity<TimelineDTO.Response> createTimeline(
            @Valid @RequestBody TimelineDTO.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(festivalService.createTimeline(request));
    }

    @Operation(summary = "Update timeline", description = "Updates a festival timeline entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timeline updated"),
            @ApiResponse(responseCode = "400", description = "Invalid timeline request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Timeline or festival day was not found")
    })
    @PatchMapping("/timelines/{timelineId}")
    public ResponseEntity<TimelineDTO.Response> updateTimeline(
            @PathVariable UUID timelineId,
            @Valid @RequestBody TimelineDTO.Request request
    ) {
        return ResponseEntity.ok(festivalService.updateTimeline(timelineId, request));
    }

    @Operation(summary = "Delete timeline", description = "Deletes a festival timeline entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Timeline deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Timeline was not found")
    })
    @DeleteMapping("/timelines/{timelineId}")
    public ResponseEntity<Void> deleteTimeline(@PathVariable UUID timelineId) {
        festivalService.deleteTimeline(timelineId);
        return ResponseEntity.noContent().build();
    }
}
