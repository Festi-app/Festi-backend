package com.festi.backend.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class MenuDTO {

    private MenuDTO() {
    }

    public record Request(
            @NotBlank @Size(max = 100) String name,
            @NotNull @Min(0) Integer price,
            String description,
            short sortOrder
    ) {
    }

    public record Response(
            UUID id,
            String name,
            int price,
            String description,
            String imageUrl,
            boolean isSoldOut,
            short sortOrder
    ) {
        public static Response from(MenuItem menuItem) {
            return new Response(
                    menuItem.getId(),
                    menuItem.getName(),
                    menuItem.getPrice(),
                    menuItem.getDescription(),
                    menuItem.getImageUrl(),
                    menuItem.isSoldOut(),
                    menuItem.getSortOrder()
            );
        }
    }
}
