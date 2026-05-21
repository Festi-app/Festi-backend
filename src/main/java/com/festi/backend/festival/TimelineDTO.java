package com.festi.backend.festival;

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
}
