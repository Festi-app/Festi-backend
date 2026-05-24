package com.festi.backend.menu;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import com.festi.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booths/{boothId}/menus")
@RequiredArgsConstructor
@Tag(name = "Menus", description = "Booth menu APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "List booth menus", description = "Returns menu items for a booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @GetMapping
    public ResponseEntity<List<MenuDTO.Response>> getMenus(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId
    ) {
        return ResponseEntity.ok(menuService.getMenus(boothId));
    }

    @Operation(summary = "Create menu item", description = "Creates a new menu item for the booth. Only allowed for NIGHT and FOOD_TRUCK booths.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Menu item created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or booth type does not support menus"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @PostMapping
    public ResponseEntity<MenuDTO.Response> createMenu(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody MenuDTO.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createMenu(currentUser, boothId, request));
    }

    @Operation(summary = "Update menu item", description = "Updates an existing menu item. Only allowed for NIGHT and FOOD_TRUCK booths.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu item updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or booth type does not support menus"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth or menu item was not found")
    })
    @PatchMapping("/{menuId}")
    public ResponseEntity<MenuDTO.Response> updateMenu(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(description = "Menu item ID") @PathVariable UUID menuId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody MenuDTO.Request request
    ) {
        return ResponseEntity.ok(menuService.updateMenu(currentUser, boothId, menuId, request));
    }

    @Operation(summary = "Delete menu item", description = "Deletes a menu item from the booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Menu item deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth or menu item was not found")
    })
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> deleteMenu(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(description = "Menu item ID") @PathVariable UUID menuId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        menuService.deleteMenu(currentUser, boothId, menuId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark menu item as sold out", description = "Marks a menu item as sold out.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu item marked as sold out"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "BOOTH_MANAGER or FESTIVAL_ADMIN role is required, and BOOTH_MANAGER must own the booth"),
            @ApiResponse(responseCode = "404", description = "Booth or menu item was not found")
    })
    @PostMapping("/{menuId}/sold-out")
    public ResponseEntity<MenuDTO.Response> markSoldOut(
            @Parameter(description = "Booth ID") @PathVariable UUID boothId,
            @Parameter(description = "Menu item ID") @PathVariable UUID menuId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(menuService.markSoldOut(currentUser, boothId, menuId));
    }
}
