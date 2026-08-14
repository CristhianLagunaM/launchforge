package com.launchforge.orders.api;

import com.launchforge.orders.api.dto.CreateOrderRequest;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.application.CancelOrderUseCase;
import com.launchforge.orders.application.CreateOrderUseCase;
import com.launchforge.orders.application.OrderQueryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderQueryService orderQueryService;
    private final CancelOrderUseCase cancelOrderUseCase;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            OrderQueryService orderQueryService,
            CancelOrderUseCase cancelOrderUseCase
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderQueryService = orderQueryService;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        OrderResponse response = createOrderUseCase.createOrder(UUID.fromString(jwt.getSubject()), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/orders/" + response.id())
                .body(response);
    }

    @GetMapping
    public List<OrderResponse> listOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderQueryService.listOrders(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        return orderQueryService.getOrder(id, UUID.fromString(jwt.getSubject()), isAdmin(authentication));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        return cancelOrderUseCase.cancelOrder(id, UUID.fromString(jwt.getSubject()), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.toString().equals("ROLE_ADMIN"));
    }
}
