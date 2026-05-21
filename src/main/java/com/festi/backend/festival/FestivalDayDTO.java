package com.festi.backend.festival;

import java.time.LocalDate;
import java.util.UUID;

public final class FestivalDayDTO {

    private FestivalDayDTO() {
    }

    public record Summary(UUID id, LocalDate day) {

        public static Summary from(FestivalDay festivalDay) {
            return new Summary(festivalDay.getId(), festivalDay.getDay());
        }
    }
}
