package com.festi.backend.festival;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public final class FestivalDTO {

    private FestivalDTO() {
    }

    public record Response(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String description
    ) {
        public static Response from(Festival festival) {
            return new Response(
                    festival.getId(),
                    festival.getName(),
                    festival.getStartDate(),
                    festival.getEndDate(),
                    festival.getDescription()
            );
        }
    }

    public record UpdateRequest(
            @NotBlank
            @Size(max = 200)
            String name,

            @NotNull
            LocalDate startDate,

            @NotNull
            LocalDate endDate,

            String description
    ) {
    }
}
