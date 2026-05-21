package com.festi.backend.favorite;

import com.festi.backend.security.AuthenticatedUser;
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
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<FavoriteDTO.Response>> getFavorites(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(favoriteService.getFavorites(currentUser.id(), currentUser.festivalId()));
    }

    @PostMapping
    public ResponseEntity<FavoriteDTO.Response> addFavorite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody FavoriteDTO.Request request
    ) {
        FavoriteDTO.Response response = favoriteService.addFavorite(
                currentUser.id(), currentUser.festivalId(), request.boothId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID favoriteId
    ) {
        favoriteService.removeFavorite(currentUser.id(), favoriteId);
        return ResponseEntity.noContent().build();
    }
}
