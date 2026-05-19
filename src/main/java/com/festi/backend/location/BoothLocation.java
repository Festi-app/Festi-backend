package com.festi.backend.location;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.entity.BaseTimeEntity;
import com.festi.backend.festival.Festival;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "booth_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_booth_locations_festival_zone_index_day",
                columnNames = {"festival_id", "zone_label", "index", "day"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothLocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id")
    private Booth booth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booth_type")
    private BoothType type;

    @Column
    private Short index;

    @Column(nullable = false)
    private LocalDate day;

    @Column(name = "zone_label", length = 100)
    private String zoneLabel;

    public BoothLocation(Festival festival, BoothType type, LocalDate day, String zoneLabel) {
        this.festival = festival;
        this.type = type;
        this.day = day;
        this.zoneLabel = zoneLabel;
    }

    public void assignBooth(Booth booth, Short index) {
        this.booth = booth;
        this.index = index;
    }

    public void removeBooth() {
        this.booth = null;
        this.index = null;
    }
}
