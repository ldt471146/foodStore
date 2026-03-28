package com.zhouri.farmshop.service;

import com.zhouri.farmshop.domain.CartItem;
import com.zhouri.farmshop.domain.InventoryMovement;
import com.zhouri.farmshop.domain.InventoryMovementType;
import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.PaymentStatus;
import com.zhouri.farmshop.repository.CartItemRepository;
import com.zhouri.farmshop.repository.InventoryMovementRepository;
import com.zhouri.farmshop.repository.OrderRepository;
import com.zhouri.farmshop.repository.ProductRepository;
import com.zhouri.farmshop.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional(readOnly = true)
    public List<CartItem> getCart(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public CartItem addToCart(Long userId, Long productId, int quantity) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数量必须大于 0");
        }

        var item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElse(CartItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .build());
        item.setQuantity(item.getQuantity() + quantity);
        return cartItemRepository.save(item);
    }

    @Transactional
    public CartItem updateCartItem(Long userId, Long itemId, int quantity) {
        var item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "购物车项不存在"));
        if (!item.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改该购物车项");
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    @Transactional
    public void deleteCartItem(Long userId, Long itemId) {
        var item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "购物车项不存在"));
        if (!item.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该购物车项");
        }
        cartItemRepository.delete(item);
    }

    @Transactional
    public Order checkout(Long userId, CheckoutCommand command) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        var cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购物车为空");
        }

        var order = Order.builder()
                .code(generateOrderCode())
                .user(user)
                .recipientName(command.recipientName().trim())
                .recipientPhone(command.recipientPhone().trim())
                .recipientAddress(command.recipientAddress().trim())
                .note(command.note())
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            var product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, product.getName() + " 库存不足");
            }
            var subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(subtotal);
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(subtotal)
                    .build());
        }
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional
    public Order pay(Long userId, Long orderId) {
        var order = getOwnedOrder(userId, orderId);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return order;
        }
        for (OrderItem item : order.getItems()) {
            var product = item.getProduct();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, product.getName() + " 库存不足，无法支付");
            }
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
            inventoryMovementRepository.save(InventoryMovement.builder()
                    .product(product)
                    .type(InventoryMovementType.OUTBOUND)
                    .quantity(item.getQuantity())
                    .source("订单 " + order.getCode())
                    .remark("用户支付后自动扣减库存")
                    .build());
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PROCESSING);
        order.setPaidAt(LocalDateTime.now());
        cartItemRepository.deleteByUserId(userId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmReceipt(Long userId, Long orderId) {
        var order = getOwnedOrder(userId, orderId);
        if (order.getStatus() == OrderStatus.RECEIVED) {
            return order;
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单尚未送达，暂不能确认收货");
        }
        order.setStatus(OrderStatus.RECEIVED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Order getOwnedOrder(Long userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该订单");
        }
        return order;
    }

    private String generateOrderCode() {
        return "FS" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
    }

    public record CheckoutCommand(
            String recipientName,
            String recipientPhone,
            String recipientAddress,
            String note
    ) {
    }
}
