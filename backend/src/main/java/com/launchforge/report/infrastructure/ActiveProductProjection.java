package com.launchforge.report.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;

public interface ActiveProductProjection {
    UUID getId();
    String getSku();
    String getName();
    String getCategory();
    BigDecimal getPrice();
}

