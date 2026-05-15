package com.festi.backend.domain.menu;

import com.festi.backend.common.entity.BaseTimeEntity;
import com.festi.backend.domain.booth.Booth;
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
@Table(name = "menu_items")
public class MenuItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_sold_out", nullable = false)
    private boolean isSoldOut = false;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    protected MenuItem() {}

    public MenuItem(Booth booth, String name, int price, String description,
                    String imageUrl, short sortOrder) {
        this.booth = booth;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public Booth getBooth() { return booth; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public boolean isSoldOut() { return isSoldOut; }
    public short getSortOrder() { return sortOrder; }

    public void update(String name, int price, String description, String imageUrl, short sortOrder) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public void markSoldOut() { this.isSoldOut = true; }
    public void markAvailable() { this.isSoldOut = false; }
}
