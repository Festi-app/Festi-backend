package com.festi.backend.festival;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class TimelineDTO {

    private TimelineDTO() {
    }

    public record Response(
            UUID id,
            LocalDate day,
            String title,
            String artist,
            LocalTime startTime,
            LocalTime endTime
    ) {
        public static Response from(Timeline timeline) {
            return new Response(
                    timeline.getId(),
                    timeline.getDay(),
                    timeline.getTitle(),
                    timeline.getArtist(),
                    timeline.getStartTime(),
                    timeline.getEndTime()
            );
        }
    }
}
