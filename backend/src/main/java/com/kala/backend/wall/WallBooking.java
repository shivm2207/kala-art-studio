
package com.kala.backend.wall;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wall_bookings")
public class WallBooking {

    public enum Status {
        REQUESTED, CONFIRMED, DECLINED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "wall_size_sqft", nullable = false, precision = 7, scale = 2)
    private BigDecimal wallSizeSqft;

    @Column(name = "rate_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal rateApplied;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "service_address", nullable = false, length = 500)
    private String serviceAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected WallBooking() {}

    public WallBooking(
            Long customerId,
            LocalDate bookingDate,
            BigDecimal wallSizeSqft,
            BigDecimal rateApplied,
            BigDecimal totalPrice,
            String serviceAddress) {
        this.customerId = customerId;
        this.bookingDate = bookingDate;
        this.wallSizeSqft = wallSizeSqft;
        this.rateApplied = rateApplied;
        this.totalPrice = totalPrice;
        this.serviceAddress = serviceAddress;
        this.status = Status.REQUESTED;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public LocalDate getBookingDate() { return bookingDate; }
    public BigDecimal getWallSizeSqft() { return wallSizeSqft; }
    public BigDecimal getRateApplied() { return rateApplied; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getServiceAddress() { return serviceAddress; }
    public Status getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void changeStatus(Status status) {
        this.status = status;
    }
}
