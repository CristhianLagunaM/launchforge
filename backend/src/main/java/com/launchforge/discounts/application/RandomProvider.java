package com.launchforge.discounts.application;

import java.util.UUID;

public interface RandomProvider {

    boolean isWinningOrder(UUID orderId, UUID customerId);
}
