package com.festi.backend.booth;

import java.util.UUID;

public final class BoothDTO {

    private BoothDTO() {
    }

    public record Summary(
            UUID id,
            String name,
            BoothCategory category,
            BoothType type,
            String imageUrl,
            boolean isWaitingOpen
    ) {
        public static Summary from(Booth booth) {
            return new Summary(
                    booth.getId(),
                    booth.getName(),
                    booth.getCategory(),
                    booth.getType(),
                    booth.getImageUrl(),
                    booth.isWaitingOpen()
            );
        }
    }

    public record Detail(
            UUID id,
            String name,
            BoothCategory category,
            BoothType type,
            String description,
            String operatingHours,
            String imageUrl,
            boolean isWaitingOpen
    ) {
        public static Detail from(Booth booth) {
            return new Detail(
                    booth.getId(),
                    booth.getName(),
                    booth.getCategory(),
                    booth.getType(),
                    booth.getDescription(),
                    booth.getOperatingHours(),
                    booth.getImageUrl(),
                    booth.isWaitingOpen()
            );
        }
    }
}
