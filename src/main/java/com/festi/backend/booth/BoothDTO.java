package com.festi.backend.booth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class BoothDTO {

    private BoothDTO() {
    }

    public record Summary(
            UUID id,
            String name,
            BoothCategory category,
            BoothType type,
            String description,
            String imageUrl,
            boolean isWaitingOpen
    ) {
        public static Summary from(Booth booth) {
            return new Summary(
                    booth.getId(),
                    booth.getName(),
                    booth.getCategory(),
                    booth.getType(),
                    booth.getDescription(),
                    booth.getImageUrl(),
                    booth.isWaitingOpen()
            );
        }
    }

    public record CreateFoodTruckRequest(
            @NotBlank @Size(max = 100) String name,
            BoothCategory category,
            String description,
            @Size(max = 100) String operatingHours,
            @Size(max = 500) String imageUrl
    ) {
    }

    public record UpdateFoodTruckRequest(
            @Size(max = 100) String name,
            BoothCategory category,
            String description,
            @Size(max = 100) String operatingHours,
            @Size(max = 500) String imageUrl
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull BoothCategory category,
            String description,
            @Size(max = 100) String operatingHours,
            @Size(max = 500) String imageUrl
    ) {
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
