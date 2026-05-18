package com.festi.backend.menu;

import java.util.UUID;

public final class MenuDTO {

    private MenuDTO() {
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
