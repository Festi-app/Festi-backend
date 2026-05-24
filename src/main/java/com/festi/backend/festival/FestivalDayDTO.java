package com.festi.backend.festival;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class FestivalDayDTO {

    private FestivalDayDTO() {
    }

    public record Summary(UUID id, LocalDate day) {

        public static Summary from(FestivalDay festivalDay) {
            return new Summary(festivalDay.getId(), festivalDay.getDay());
        }
    }

    public record Request(
            @NotNull
            LocalDate day,

            @NotNull
            LocalTime dayStart,

            @NotNull
            LocalTime dayEnd,

            @NotNull
            LocalTime nightStart,

            @NotNull
            LocalTime nightEnd
    ) {
    }

    public record Response(
            UUID id,
            LocalDate day,
            LocalTime dayStart,
            LocalTime dayEnd,
            LocalTime nightStart,
            LocalTime nightEnd
    ) {
        public static Response from(FestivalDay festivalDay) {
            return new Response(
                    festivalDay.getId(),
                    festivalDay.getDay(),
                    festivalDay.getDayStart(),
                    festivalDay.getDayEnd(),
                    festivalDay.getNightStart(),
                    festivalDay.getNightEnd()
            );
        }
    }
}
