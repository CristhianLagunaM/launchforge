package com.launchforge.discounts.api;

import com.launchforge.discounts.api.dto.DiscountConfigurationUpdateRequest;
import com.launchforge.discounts.api.dto.DiscountConfigurationView;
import com.launchforge.discounts.application.DiscountConfigurationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discount-configurations")
@PreAuthorize("hasRole('ADMIN')")
public class DiscountConfigurationController {

    private final DiscountConfigurationService discountConfigurationService;

    public DiscountConfigurationController(DiscountConfigurationService discountConfigurationService) {
        this.discountConfigurationService = discountConfigurationService;
    }

    @GetMapping
    public List<DiscountConfigurationView> listConfigurations() {
        return discountConfigurationService.listConfigurations();
    }

    @PatchMapping("/{code}")
    public DiscountConfigurationView updateConfiguration(
            @PathVariable String code,
            @Valid @RequestBody DiscountConfigurationUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return discountConfigurationService.updateConfiguration(code, request, UUID.fromString(jwt.getSubject()));
    }
}
