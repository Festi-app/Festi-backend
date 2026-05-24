package com.festi.backend.waiting;

import com.festi.backend.booth.BoothDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class WaitingDTO {

    private WaitingDTO() {
    }

    public record Request(@Min(1) short partySize) {
    }

    public record StatusRequest(@NotNull WaitingStatus status) {
    }

    public record OpenStatusRequest(@NotNull Boolean open) {
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
