package com.festi.backend.booth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class BoothApplicationDTO {

    private BoothApplicationDTO() {
    }

    public record CreateRequest(
            @NotBlank
            @Size(max = 30)
            String id,

            @NotBlank
            @Size(min = 8, max = 100)
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,100}$",
                    message = "Password must contain uppercase, lowercase, number, and special character."
            )
            String password,

            @NotBlank
            @Size(max = 100)
            String name,

            @NotBlank
            @Size(max = 20)
            String phone,

            @NotBlank
            @Size(max = 100)
            String boothName,

            @NotNull
            BoothType boothType,

            BoothCategory boothCategory,

            String description
    ) {
    }

    public record RejectRequest(
            String reviewMemo
    ) {
    }

    public record Response(
            UUID id,
            UUID festivalId,
            String applicantId,
            UUID boothId,
            String boothName,
            BoothType boothType,
            BoothCategory boothCategory,
            String description,
            BoothApplicationStatus status,
            String reviewMemo,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static Response from(BoothApplication application) {
            return new Response(
                    application.getId(),
                    application.getFestival().getId(),
                    application.getApplicantId(),
                    application.getBooth() == null ? null : application.getBooth().getId(),
                    application.getBoothName(),
                    application.getBoothType(),
                    application.getBoothCategory(),
                    application.getDescription(),
                    application.getStatus(),
                    application.getReviewMemo(),
                    application.getCreatedAt(),
                    application.getUpdatedAt()
            );
        }
    }
}
