
package com.kala.backend.paintings;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "painting_orders")
public class PaintingOrder {

    public enum Status {
        PLACED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "painting_id", nullable = false)
    private Long paintingId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "price_at_purchase", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected PaintingOrder() {}

    public PaintingOrder(
            Long paintingId,
            Long customerId,
            BigDecimal priceAtPurchase) {
        this.paintingId = paintingId;
        this.customerId = customerId;
        this.priceAtPurchase = priceAtPurchase;
        this.status = Status.PLACED;
    }

    public Long getId() { return id; }
    public Long getPaintingId() { return paintingId; }
    public Long getCustomerId() { return customerId; }
    public Status getStatus() { return status; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }

    public void cancel(OffsetDateTime time) {
        this.status = Status.CANCELLED;
        this.cancelledAt = time;
    }
}
