package com.festi.backend.location;

import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothType;
import java.time.LocalDate;

public final class LocationDTO {

    private LocationDTO() {
    }

    public record Response(
            Short id,
            BoothType type,
            Short index,
            LocalDate day,
            String zoneLabel,
            BoothDTO.Summary boothSummary
    ) {
        public static Response from(BoothLocation location) {
            return new Response(
                    location.getId(),
                    location.getType(),
                    location.getIndex(),
                    location.getDay(),
                    location.getZoneLabel(),
                    location.getBooth() == null ? null : BoothDTO.Summary.from(location.getBooth())
            );
        }
    }
}
