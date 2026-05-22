package com.festi.backend.location;

import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.FestivalDayDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class LocationDTO {

    private LocationDTO() {
    }

    public record Response(
            Short id,
            BoothType type,
            Short index,
            FestivalDayDTO.Summary festivalDay,
            String zoneLabel,
            BoothDTO.Summary boothSummary
    ) {
        public static Response from(BoothLocation location) {
            return new Response(
                    location.getId(),
                    location.getType(),
                    location.getIndex(),
                    FestivalDayDTO.Summary.from(location.getDay()),
                    location.getZoneLabel(),
                    location.getBooth() == null ? null : BoothDTO.Summary.from(location.getBooth())
            );
        }
    }

    public record SlotsRequest(
            @NotNull
            UUID festivalDayId,

            @NotNull
            BoothType type,

            @NotEmpty
            List<@Valid ZoneSlotsRequest> zones
    ) {
    }

    public record ZoneSlotsRequest(
            @NotBlank
            @Size(max = 100)
            String zoneLabel,

            @Min(1)
            short count
    ) {
    }

    public record AssignmentRequest(
            @NotNull
            UUID boothId
    ) {
    }
}
