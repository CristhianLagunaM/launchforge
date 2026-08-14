package com.launchforge.catalog.api.dto;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull Boolean active
) {
}
