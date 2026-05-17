package com.festi.backend.user;

import com.festi.backend.auth.AuthDTO;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class UserDTO {

    private UserDTO() {
    }

    public record Response(
            UUID id,
            String email,
            String name,
            String phone,
            UserRole role
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getPhone(),
                    user.getRole()
            );
        }
    }

    public record UpdateRequest(
            @Email
            @Size(max = 255)
            @Pattern(regexp = ".*\\S.*", message = "Email must not be blank.")
            String email,

            @Size(max = 100)
            @Pattern(regexp = ".*\\S.*", message = "Name must not be blank.")
            String name,

            @Size(max = 20)
            @Pattern(regexp = ".*\\S.*", message = "Phone must not be blank.")
            String phone
    ) {
        @AssertTrue(message = "At least one field must be provided.")
        public boolean hasAnyField() {
            return email != null || name != null || phone != null;
        }
    }

    public record UpdateResponse(
            Response user,
            AuthDTO.TokenResponse token
    ) {
    }
}
