package com.festi.backend.booth;

import com.festi.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "booths")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "manager_id", length = 30)
    private String managerId;

    @Column(name = "created_by_id", nullable = false, length = 30)
    private String createdById;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booth_category")
    private BoothCategory category = BoothCategory.ACTIVITY;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booth_type")
    private BoothType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "operating_hours", length = 100)
    private String operatingHours;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_waiting_open", nullable = false)
    private boolean isWaitingOpen = false;

    public Booth(String name, BoothCategory category, BoothType type, String createdById) {
        this.name = name;
        this.category = category;
        this.type = type;
        this.createdById = createdById;
    }

    public void update(String name, BoothCategory category, String description,
                       String operatingHours, String imageUrl) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.operatingHours = operatingHours;
        this.imageUrl = imageUrl;
    }

    public void assignManager(String managerId) { this.managerId = managerId; }
    public void openWaiting() { this.isWaitingOpen = true; }
    public void closeWaiting() { this.isWaitingOpen = false; }
}
