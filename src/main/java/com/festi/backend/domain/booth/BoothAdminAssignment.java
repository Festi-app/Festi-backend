package com.festi.backend.domain.booth;

import com.festi.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "booth_admin_assignments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"booth_id", "user_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class BoothAdminAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BoothAdminAssignment() {}

    public BoothAdminAssignment(Booth booth, User user, User grantedBy) {
        this.booth = booth;
        this.user = user;
        this.grantedBy = grantedBy;
    }

    public UUID getId() { return id; }
    public Booth getBooth() { return booth; }
    public User getUser() { return user; }
    public User getGrantedBy() { return grantedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
