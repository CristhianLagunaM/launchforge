package com.launchforge.report.api.dto;

import java.util.UUID;

public record TopCustomerReport(
        UUID customerId,
        String email,
        String firstName,
        String lastName,
        long orderCount
) {
}

