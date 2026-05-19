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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "festival_days",
        uniqueConstraints = @UniqueConstraint(columnNames = {"festival_id", "day"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalDay extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false)
    private LocalDate day;

    @Column(name = "day_start")
    private LocalTime dayStart;

    @Column(name = "day_end")
    private LocalTime dayEnd;

    @Column(name = "night_start")
    private LocalTime nightStart;

    @Column(name = "night_end")
    private LocalTime nightEnd;

    public FestivalDay(Festival festival, LocalDate day) {
        this.festival = festival;
        this.day = day;
    }

    public void updateHours(LocalTime dayStart, LocalTime dayEnd, LocalTime nightStart, LocalTime nightEnd) {
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
        this.nightStart = nightStart;
        this.nightEnd = nightEnd;
    }
}
