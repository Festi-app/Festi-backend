package com.festi.backend.location;

import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.FestivalDayDTO;

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
}
