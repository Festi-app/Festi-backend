package com.festi.backend.waiting;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
