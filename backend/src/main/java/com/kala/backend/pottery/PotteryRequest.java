
package com.kala.backend.pottery;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pottery_requests")
public class PotteryRequest {

    public enum Status {
        REQUESTED, ACCEPTED, DECLINED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "item_id", nullable = false, length = 10)
    private String itemId;

    @Column(name = "glaze_id", nullable = false, length = 10)
    private String glazeId;

    @Column(nullable = false)
    private Short quantity;

    @Column(name = "needed_by", nullable = false)
    private LocalDate neededBy;

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

    protected PotteryRequest() {}

    public PotteryRequest(
            Long customerId,
            String itemId,
            String glazeId,
            Short quantity,
            LocalDate neededBy) {
        this.customerId = customerId;
        this.itemId = itemId;
        this.glazeId = glazeId;
        this.quantity = quantity;
        this.neededBy = neededBy;
        this.status = Status.REQUESTED;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getItemId() { return itemId; }
    public String getGlazeId() { return glazeId; }
    public Short getQuantity() { return quantity; }
    public LocalDate getNeededBy() { return neededBy; }
    public Status getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void changeStatus(Status status) {
        this.status = status;
    }

    public void updateDetails(String itemId, String glazeId, Short quantity, LocalDate neededBy) {
        this.itemId = itemId;
        this.glazeId = glazeId;
        this.quantity = quantity;
        this.neededBy = neededBy;
    }
}
