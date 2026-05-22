package com.festi.backend.auth;

import com.festi.backend.user.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication and account registration APIs")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Sign up", description = "Creates a user account for the active festival.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User account created"),
            @ApiResponse(responseCode = "400", description = "Invalid signup request"),
            @ApiResponse(responseCode = "409", description = "ID is already in use")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserDTO.Response> signup(@Valid @RequestBody AuthDTO.SignupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserDTO.Response.from(authService.signup(request)));
    }

    @Operation(summary = "Log in", description = "Authenticates credentials and returns a JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded"),
            @ApiResponse(responseCode = "400", description = "Invalid login request"),
            @ApiResponse(responseCode = "401", description = "Invalid ID or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthDTO.TokenResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
