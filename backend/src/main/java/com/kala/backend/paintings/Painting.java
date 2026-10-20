package com.kala.backend.paintings;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "paintings")
public class Painting {

    public enum Status {
        AVAILABLE, SOLD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seed_id", unique = true, length = 20)
    private String seedId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String artist;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected Painting() {}

    public Long getId() { return id; }
    public String getSeedId() { return seedId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public Status getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void updateDetails(
            String title,
            String artist,
            String description,
            BigDecimal price,
            String imageUrl,
            Status status) {
        this.title = title;
        this.artist = artist;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public static Painting create(
            String title,
            String artist,
            String description,
            BigDecimal price,
            String imageUrl) {
        Painting painting = new Painting();
        painting.title = title;
        painting.artist = artist;
        painting.description = description;
        painting.price = price;
        painting.imageUrl = imageUrl;
        painting.status = Status.AVAILABLE;
        return painting;
    }
        /**
     * Creates a painting from seed data while preserving its seed ID
     * and its intended AVAILABLE or SOLD status.
     */
    public static Painting createSeeded(
            String seedId,
            String title,
            String artist,
            String description,
            BigDecimal price,
            String imageUrl,
            Status status) {

        Painting painting = create(
                title,
                artist,
                description,
                price,
                imageUrl
        );

        painting.seedId = seedId;
        painting.status = status;

        return painting;
    }
}