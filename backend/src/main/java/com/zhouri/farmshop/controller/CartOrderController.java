package com.zhouri.farmshop.controller;

import com.zhouri.farmshop.domain.CartItem;
import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import com.zhouri.farmshop.security.AuthenticatedUser;
import com.zhouri.farmshop.service.CatalogService;
import com.zhouri.farmshop.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartOrderController {

    private final OrderService orderService;
    private final CatalogService catalogService;

    @GetMapping("/cart")
    public List<CartItemResponse> cart(Authentication authentication) {
        return orderService.getCart(currentUser(authentication).id()).stream()
                .map(this::toCartItemResponse)
                .toList();
    }

    @PostMapping("/cart/items")
    public CartItemResponse addCartItem(@Valid @RequestBody CartItemRequest request, Authentication authentication) {
        return toCartItemResponse(orderService.addToCart(currentUser(authentication).id(), request.productId(), request.quantity()));
    }

    @PatchMapping("/cart/items/{itemId}")
    public CartItemResponse updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication
    ) {
        var item = orderService.updateCartItem(currentUser(authentication).id(), itemId, request.quantity());
        return item == null ? null : toCartItemResponse(item);
    }

    @DeleteMapping("/cart/items/{itemId}")
    public void deleteCartItem(@PathVariable Long itemId, Authentication authentication) {
        orderService.deleteCartItem(currentUser(authentication).id(), itemId);
    }

    @PostMapping("/orders/checkout")
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request, Authentication authentication) {
        var order = orderService.checkout(currentUser(authentication).id(), new OrderService.CheckoutCommand(
                request.recipientName(),
                request.recipientPhone(),
                request.recipientAddress(),
                request.note()
        ));
        return toOrderResponse(order);
    }

    @PostMapping("/orders/{orderId}/pay")
    public OrderResponse pay(@PathVariable Long orderId, Authentication authentication) {
        return toOrderResponse(orderService.pay(currentUser(authentication).id(), orderId));
    }

    @PostMapping("/orders/{orderId}/confirm-receipt")
    public OrderResponse confirmReceipt(@PathVariable Long orderId, Authentication authentication) {
        return toOrderResponse(orderService.confirmReceipt(currentUser(authentication).id(), orderId));
    }

    @GetMapping("/orders")
    public List<OrderResponse> myOrders(Authentication authentication) {
        return orderService.listOrders(currentUser(authentication).id()).stream().map(this::toOrderResponse).toList();
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse orderDetail(@PathVariable Long orderId, Authentication authentication) {
        return toOrderResponse(orderService.getOwnedOrder(currentUser(authentication).id(), orderId));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCode(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getRecipientAddress(),
                order.getLogisticsCompany(),
                order.getTrackingNumber(),
            order.getCreatedAt(),
            order.getPaidAt(),
            order.getShippedAt(),
            order.getDeliveredAt(),
            order.getStatus() == com.zhouri.farmshop.domain.OrderStatus.RECEIVED ? order.getUpdatedAt() : null,
            order.getItems().stream().map(this::toOrderItemResponse).toList()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        var eligibility = catalogService.getOrderItemReviewEligibility(item.getOrder().getUser().getId(), item);
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProduct().getCreatedBy().getFullName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                eligibility.canReview(),
                eligibility.reviewed(),
                eligibility.message()
        );
    }

    public record CartItemRequest(Long productId, @Min(1) Integer quantity) {
    }

    public record UpdateCartItemRequest(@Min(0) Integer quantity) {
    }

    public record CheckoutRequest(
            @NotBlank String recipientName,
            @NotBlank String recipientPhone,
            @NotBlank String recipientAddress,
            String note
    ) {
    }

    public record CartItemResponse(
            Long id,
            Long productId,
            String productName,
            String imageUrl,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal
    ) {
    }

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            String publisherName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal,
            boolean canReview,
            boolean reviewed,
            String reviewMessage
    ) {
    }

    public record OrderResponse(
            Long id,
            String code,
            com.zhouri.farmshop.domain.OrderStatus status,
            com.zhouri.farmshop.domain.PaymentStatus paymentStatus,
            BigDecimal totalAmount,
            String recipientName,
            String recipientPhone,
            String recipientAddress,
            String logisticsCompany,
            String trackingNumber,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt,
            LocalDateTime receivedAt,
            List<OrderItemResponse> items
    ) {
    }
}
