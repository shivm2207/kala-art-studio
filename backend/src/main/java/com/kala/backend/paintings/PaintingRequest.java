package com.kala.backend.paintings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaintingRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 120)
        String artist,

        @NotBlank
        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal price,

        @Size(max = 255)
        String imageUrl,

        Painting.Status status
) {}