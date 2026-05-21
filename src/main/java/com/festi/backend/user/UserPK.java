package com.festi.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPK implements Serializable {

    @Column(name = "festival_id", nullable = false)
    private UUID festivalId;

    @Column(nullable = false, length = 30)
    private String id;

    public UserPK(UUID festivalId, String id) {
        this.festivalId = festivalId;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPK other)) return false;
        return Objects.equals(festivalId, other.festivalId) && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(festivalId, id);
    }
}
