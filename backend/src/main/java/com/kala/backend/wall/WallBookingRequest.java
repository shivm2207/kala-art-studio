
package com.kala.backend.wall;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record WallBookingRequest(
        @NotNull
        @FutureOrPresent
        LocalDate bookingDate,

        @NotNull
        @DecimalMin("10.00")
        @DecimalMax("500.00")
        @Digits(integer = 5, fraction = 2, message = "Wall size can have at most 2 decimal places.")
        BigDecimal wallSizeSqft,

        @NotBlank
        @Size(max = 500)
        String serviceAddress
) {}
