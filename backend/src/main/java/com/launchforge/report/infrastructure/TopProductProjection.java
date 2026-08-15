package com.launchforge.report.infrastructure;

import java.util.UUID;

public interface TopProductProjection {
    UUID getProductId();
    String getSku();
    String getName();
    Long getQuantitySold();
}

