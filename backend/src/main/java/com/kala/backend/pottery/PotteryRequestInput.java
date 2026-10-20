
package com.kala.backend.pottery;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PotteryRequestInput(
        @NotBlank @Size(max = 10)
        String itemId,

        @NotBlank @Size(max = 10)
        String glazeId,

        @NotNull @Min(1) @Max(12)
        Short quantity,

        @NotNull @FutureOrPresent
        LocalDate neededBy
) {}
