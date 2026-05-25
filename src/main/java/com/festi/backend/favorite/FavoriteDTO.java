package com.festi.backend.favorite;

import com.festi.backend.booth.BoothDTO;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class FavoriteDTO {

    private FavoriteDTO() {
    }

    public record Request(UUID boothId) {
    }

    public record Response(
            UUID id,
            BoothDTO.Summary boothSummary,
            OffsetDateTime createdAt
    ) {
        public static Response from(Favorite favorite) {
            return new Response(
                    favorite.getId(),
                    BoothDTO.Summary.from(favorite.getBooth()),
                    favorite.getCreatedAt()
            );
        }

        public static Response from(Favorite favorite, Integer waitingTeamCount) {
            return new Response(
                    favorite.getId(),
                    BoothDTO.Summary.from(favorite.getBooth(), waitingTeamCount),
                    favorite.getCreatedAt()
            );
        }
    }
}
