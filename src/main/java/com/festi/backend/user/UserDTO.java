package com.festi.backend.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDTO {

    private UserDTO() {
    }

    public record Response(
            String id,
            String name,
            String phone,
            UserRole role
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getName(),
                    user.getPhone(),
                    user.getRole()
            );
        }
    }

    public record UpdateRequest(
            @Size(max = 100)
            @Pattern(regexp = ".*\\S.*", message = "Name must not be blank.")
            String name,

            @Size(max = 20)
            @Pattern(regexp = ".*\\S.*", message = "Phone must not be blank.")
            String phone
    ) {
        @AssertTrue(message = "At least one field must be provided.")
        public boolean hasAnyField() {
            return name != null || phone != null;
        }
    }
}
