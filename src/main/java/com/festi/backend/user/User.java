package com.festi.backend.user;

import com.festi.backend.common.entity.BaseTimeEntity;
import com.festi.backend.festival.Festival;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @EmbeddedId
    private UserPK pk;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("festivalId")
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_role")
    private UserRole role = UserRole.USER;

    public User(Festival festival, String id, String passwordHash, String name, String phone) {
        this.pk = new UserPK(festival.getId(), id);
        this.festival = festival;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
    }

    public String getId() {
        return pk.getId();
    }

    public UUID getFestivalId() {
        return pk.getFestivalId();
    }

    public void updateProfile(String name, String phone) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }
}
