package com.festi.backend.waiting;

import com.festi.backend.booth.BoothDTO;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class WaitingDTO {

    private WaitingDTO() {
    }

    public record Response(
            UUID id,
            BoothDTO.Summary boothSummary,
            short partySize,
            WaitingStatus status,
            short callCount,
            OffsetDateTime registeredAt
    ) {
        public static Response from(Waiting waiting) {
            return new Response(
                    waiting.getId(),
                    BoothDTO.Summary.from(waiting.getBooth()),
                    waiting.getPartySize(),
                    waiting.getStatus(),
                    waiting.getCallCount(),
                    waiting.getRegisteredAt()
            );
        }
    }
}
