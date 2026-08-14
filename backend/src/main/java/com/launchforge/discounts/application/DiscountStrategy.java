package com.launchforge.discounts.application;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import java.util.Optional;

public interface DiscountStrategy {

    DiscountCode code();

    int applicationOrder();

    boolean isApplicable(DiscountContext context, DiscountConfiguration configuration);

    Optional<DiscountApplication> apply(DiscountContext context, DiscountConfiguration configuration);
}
