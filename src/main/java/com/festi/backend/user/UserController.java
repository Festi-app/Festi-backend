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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Authenticated user profile APIs")
@SecurityRequirement(name = BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile for the current festival.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "User was not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDTO.Response> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(userService.getMe(currentUser.id(), currentUser.festivalId()));
    }

    @Operation(summary = "Update my profile", description = "Updates the authenticated user's profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid update request"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "User was not found")
    })
    @PatchMapping("/me")
    public ResponseEntity<UserDTO.Response> updateMe(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UserDTO.UpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateMe(currentUser.id(), currentUser.festivalId(), request));
    }


}
