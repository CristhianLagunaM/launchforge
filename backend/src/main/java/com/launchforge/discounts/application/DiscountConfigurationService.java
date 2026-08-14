package com.launchforge.discounts.application;

import com.launchforge.discounts.api.dto.DiscountConfigurationUpdateRequest;
import com.launchforge.discounts.api.dto.DiscountConfigurationView;
import com.launchforge.discounts.infrastructure.DiscountConfigurationRepository;
import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import com.launchforge.shared.exception.ApiBadRequestException;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscountConfigurationService {

    private final DiscountConfigurationRepository discountConfigurationRepository;

    public DiscountConfigurationService(DiscountConfigurationRepository discountConfigurationRepository) {
        this.discountConfigurationRepository = discountConfigurationRepository;
    }

    @Transactional(readOnly = true)
    public Map<DiscountCode, DiscountConfiguration> getEnabledConfigurationsByCode() {
        Map<DiscountCode, DiscountConfiguration> configurations = new EnumMap<>(DiscountCode.class);
        for (DiscountConfiguration configuration : discountConfigurationRepository.findAllByOrderByCodeAsc()) {
            if (Boolean.TRUE.equals(configuration.getEnabled())) {
                DiscountConfigurationRules.validateForExecution(configuration);
                configurations.put(DiscountCode.valueOf(configuration.getCode()), configuration);
            }
        }
        return configurations;
    }

    @Transactional(readOnly = true)
    public List<DiscountConfigurationView> listConfigurations() {
        return discountConfigurationRepository.findAllByOrderByCodeAsc().stream()
                .map(DiscountConfigurationView::from)
                .toList();
    }

    @Transactional
    public DiscountConfigurationView updateConfiguration(String code, DiscountConfigurationUpdateRequest request, UUID updatedBy) {
        DiscountConfiguration configuration = discountConfigurationRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Discount configuration not found",
                        "Discount configuration not found for code: " + code,
                        "discounts/configuration-not-found"
                ));

        configuration.setEnabled(request.enabled());
        configuration.setPercentage(scalePercentage(request.percentage()));
        configuration.setStartAt(request.startAt());
        configuration.setEndAt(request.endAt());
        configuration.setMinimumOrders(request.minimumOrders());
        configuration.setLookbackMonths(request.lookbackMonths());
        configuration.setUpdatedBy(updatedBy);

        validateForUpdate(configuration);
        return DiscountConfigurationView.from(discountConfigurationRepository.saveAndFlush(configuration));
    }

    private void validateForUpdate(DiscountConfiguration configuration) {
        try {
            DiscountConfigurationRules.validateForExecution(configuration);
        } catch (ApiConflictException exception) {
            throw new ApiBadRequestException(
                    exception.getTitle(),
                    exception.getMessage(),
                    "discounts/invalid-configuration"
            );
        }
    }

    private BigDecimal scalePercentage(BigDecimal percentage) {
        return percentage == null ? null : percentage.setScale(2, RoundingMode.HALF_UP);
    }
}
