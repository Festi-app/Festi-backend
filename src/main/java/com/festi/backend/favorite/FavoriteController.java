package com.festi.backend.favorite;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "User favorite booth APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "List my favorites", description = "Returns the authenticated user's favorite booths.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required")
    })
    @GetMapping
    public ResponseEntity<List<FavoriteDTO.Response>> getFavorites(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(favoriteService.getFavorites(currentUser.id(), currentUser.festivalId()));
    }

    @Operation(summary = "Add favorite", description = "Adds a booth to the authenticated user's favorites.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Favorite created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or favorite limit reached"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "Booth or festival was not found"),
            @ApiResponse(responseCode = "409", description = "Booth is already in favorites")
    })
    @PostMapping
    public ResponseEntity<FavoriteDTO.Response> addFavorite(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody FavoriteDTO.Request request
    ) {
        FavoriteDTO.Response response = favoriteService.addFavorite(
                currentUser.id(), currentUser.festivalId(), request.boothId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove favorite", description = "Removes one favorite owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Favorite removed"),
            @ApiResponse(responseCode = "400", description = "Invalid favorite ID"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "USER role is required"),
            @ApiResponse(responseCode = "404", description = "Favorite was not found")
    })
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> removeFavorite(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Favorite ID")
            @PathVariable UUID favoriteId
    ) {
        favoriteService.removeFavorite(currentUser.id(), favoriteId);
        return ResponseEntity.noContent().build();
    }
}
