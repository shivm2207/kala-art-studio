
package com.kala.backend.wall;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "mural_rate")
public class MuralRate {

    @Id
    private Short id;

    @Column(name = "rate_per_sqft", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal ratePerSqft;

    protected MuralRate() {}

    public MuralRate(BigDecimal ratePerSqft) {
        this.id = 1;
        this.ratePerSqft = ratePerSqft;
    }

    public Short getId() { return id; }
    public BigDecimal getRatePerSqft() { return ratePerSqft; }

    public void changeRate(BigDecimal ratePerSqft) {
        this.ratePerSqft = ratePerSqft;
    }
}
