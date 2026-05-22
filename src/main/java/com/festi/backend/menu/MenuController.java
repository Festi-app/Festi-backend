package com.festi.backend.menu;

import static com.festi.backend.config.OpenApiConfig.BEARER_AUTH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booths/{boothId}/menus")
@Tag(name = "Menus", description = "Booth menu APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "List booth menus", description = "Returns menu items for a booth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu list retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid booth ID"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Booth was not found")
    })
    @GetMapping
    public ResponseEntity<List<MenuDTO.Response>> getMenus(
            @Parameter(description = "Booth ID")
            @PathVariable UUID boothId
    ) {
        return ResponseEntity.ok(menuService.getMenus(boothId));
    }
}
