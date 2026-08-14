package com.launchforge.catalog.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpsertRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 200) String slug,
        @NotBlank String description,
        @NotNull Long categoryId,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal price
) {
}
