package com.festi.backend.festival;

import com.festi.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "timelines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Timeline extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false)
    private LocalDate day;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String artist;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public Timeline(Festival festival, LocalDate day, String title, String artist,
                    LocalTime startTime, LocalTime endTime) {
        this.festival = festival;
        this.day = day;
        this.title = title;
        this.artist = artist;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void update(String title, String artist, LocalTime startTime, LocalTime endTime) {
        this.title = title;
        this.artist = artist;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
