package com.festi.backend.booth;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "booth_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(name = "applicant_id", nullable = false, length = 30)
    private String applicantId;

    @Column(name = "booth_name", nullable = false, length = 100)
    private String boothName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "booth_type", nullable = false, columnDefinition = "booth_type")
    private BoothType boothType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "booth_category", nullable = false, columnDefinition = "booth_category")
    private BoothCategory boothCategory = BoothCategory.ACTIVITY;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booth_application_status")
    private BoothApplicationStatus status = BoothApplicationStatus.PENDING;

    @Column(name = "review_memo", columnDefinition = "TEXT")
    private String reviewMemo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", unique = true)
    private Booth booth;

    public BoothApplication(Festival festival, String applicantId, String boothName,
                            BoothType boothType, BoothCategory boothCategory,
                            String description) {
        this.festival = festival;
        this.applicantId = applicantId;
        this.boothName = boothName;
        this.boothType = boothType;
        this.boothCategory = boothCategory;
        this.description = description;
    }

    public void approve(Booth booth) {
        this.booth = booth;
        this.status = BoothApplicationStatus.APPROVED;
    }

    public void reject(String reviewMemo) {
        this.status = BoothApplicationStatus.REJECTED;
        this.reviewMemo = reviewMemo;
    }
}
