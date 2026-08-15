package com.launchforge.report.infrastructure;

import java.util.UUID;

public interface TopCustomerProjection {
    UUID getCustomerId();
    String getEmail();
    String getFirstName();
    String getLastName();
    Long getOrderCount();
}

