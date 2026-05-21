package com.festi.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDTO {

    private AuthDTO() {
    }

    public record SignupRequest(
            @NotBlank
            @Size(max = 30)
            String id,

            @NotBlank
            @Size(min = 8, max = 100)
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,100}$",
                    message = "Password must contain uppercase, lowercase, number, and special character."
            )
            String password,

            @NotBlank
            @Size(max = 100)
            String name,

            @NotBlank
            @Size(max = 20)
            String phone
    ) {
    }

    public record LoginRequest(
            @NotBlank
            @Size(max = 30)
            String id,

            @NotBlank
            String password
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {
    }
}
