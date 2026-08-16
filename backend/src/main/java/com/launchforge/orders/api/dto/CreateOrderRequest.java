package com.launchforge.orders.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateOrderRequest(

        @NotEmpty
        List<@Valid OrderItemRequest> items,

        @NotBlank
        @Size(max = 3000)
        String requirementDescription,

        @NotBlank
        @Size(max = 1000)
        String projectObjective,

        @NotBlank
        @Email
        @Size(max = 180)
        String contactEmail,

        @Size(max = 40)
        String contactPhone,

        LocalDate desiredDeliveryDate,

        @Size(max = 2000)
        String referencesUrl
) {
}
