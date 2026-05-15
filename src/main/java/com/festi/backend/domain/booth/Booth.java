package com.festi.backend.domain.booth;

import com.festi.backend.common.entity.BaseTimeEntity;
import com.festi.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "booths")
public class Booth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_waiting_open", nullable = false)
    private boolean isWaitingOpen = false;

    protected Booth() {}

    public Booth(String name, BoothCategory category, BoothType type, User createdBy) {
        this.name = name;
        this.category = category;
        this.type = type;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public User getManager() { return manager; }
    public User getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public BoothCategory getCategory() { return category; }
    public BoothType getType() { return type; }
    public String getDescription() { return description; }
    public String getOperatingHours() { return operatingHours; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return isActive; }
    public boolean isWaitingOpen() { return isWaitingOpen; }

    public void update(String name, BoothCategory category, String description,
                       String operatingHours, String imageUrl) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.operatingHours = operatingHours;
        this.imageUrl = imageUrl;
    }

    public void assignManager(User manager) { this.manager = manager; }
    public void deactivate() { this.isActive = false; }
    public void openWaiting() { this.isWaitingOpen = true; }
    public void closeWaiting() { this.isWaitingOpen = false; }
}
