package com.festi.backend.festival;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NoticeDTO {

    private NoticeDTO() {
    }

    public record Response(
            UUID id,
            String title,
            String content,
            boolean pinned,
            OffsetDateTime createdAt
    ) {
        public static Response from(Notice notice) {
            return new Response(
                    notice.getId(),
                    notice.getTitle(),
                    notice.getContent(),
                    notice.isPinned(),
                    notice.getCreatedAt()
            );
        }
    }
}
