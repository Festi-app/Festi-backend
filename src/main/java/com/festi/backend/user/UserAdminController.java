package com.festi.backend.user;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Festival admin user management APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class UserAdminController {

    private final UserService userService;

    @Operation(summary = "List users by role", description = "Returns users filtered by role (FESTIVAL_ADMIN or BOOTH_MANAGER).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required")
    })
    @GetMapping
    public ResponseEntity<List<UserDTO.Response>> getUsersByRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam UserRole role
    ) {
        return ResponseEntity.ok(userService.getUsersByRole(currentUser.festivalId(), role));
    }

    @Operation(summary = "Update user role", description = "Changes a user's role to FESTIVAL_ADMIN or BOOTH_MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Invalid role"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDTO.Response> updateUserRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String userId,
            @Valid @RequestBody UserDTO.RoleUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserRole(userId, currentUser.festivalId(), request.role()));
    }

    @Operation(summary = "Reset user role", description = "Resets a user's role back to USER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role reset to USER"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "FESTIVAL_ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{userId}/role")
    public ResponseEntity<UserDTO.Response> resetUserRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(userService.resetUserRole(userId, currentUser.festivalId()));
    }
}