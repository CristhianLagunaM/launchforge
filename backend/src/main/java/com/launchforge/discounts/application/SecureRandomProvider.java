package com.launchforge.discounts.application;

import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SecureRandomProvider implements RandomProvider {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean isWinningOrder(UUID orderId, UUID customerId) {
        return secureRandom.nextBoolean();
    }
}
