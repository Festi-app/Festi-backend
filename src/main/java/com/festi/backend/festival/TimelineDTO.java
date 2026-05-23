package com.festi.backend.festival;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

public final class TimelineDTO {

    private TimelineDTO() {
    }

    public record Response(
            UUID id,
            FestivalDayDTO.Summary festivalDay,
            String title,
            String artist,
            LocalTime startTime,
            LocalTime endTime
    ) {
        public static Response from(Timeline timeline, FestivalDay festivalDay) {
            return new Response(
                    timeline.getId(),
                    FestivalDayDTO.Summary.from(festivalDay),
                    timeline.getTitle(),
                    timeline.getArtist(),
                    timeline.getStartTime(),
                    timeline.getEndTime()
            );
        }
    }

    public record Request(
            @NotNull
            UUID festivalDayId,

            @NotBlank
            @Size(max = 200)
            String title,

            @NotBlank
            @Size(max = 200)
            String artist,

            @NotNull
            LocalTime startTime,

            @NotNull
            LocalTime endTime
    ) {
    }
}
