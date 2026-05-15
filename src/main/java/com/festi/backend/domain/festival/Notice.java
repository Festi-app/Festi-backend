package com.festi.backend.domain.festival;

import com.festi.backend.common.entity.BaseTimeEntity;
import com.festi.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "notices")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    protected Notice() {}

    public Notice(Festival festival, String title, String content, User createdBy) {
        this.festival = festival;
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public Festival getFestival() { return festival; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public User getCreatedBy() { return createdBy; }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
