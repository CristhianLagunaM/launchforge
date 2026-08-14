package com.launchforge.discounts.infrastructure;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountConfigurationRepository extends JpaRepository<DiscountConfiguration, UUID> {

    List<DiscountConfiguration> findAllByOrderByCodeAsc();

    Optional<DiscountConfiguration> findByCodeIgnoreCase(String code);
}
