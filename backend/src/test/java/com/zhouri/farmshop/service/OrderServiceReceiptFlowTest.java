package com.zhouri.farmshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.repository.CartItemRepository;
import com.zhouri.farmshop.repository.InventoryMovementRepository;
import com.zhouri.farmshop.repository.OrderRepository;
import com.zhouri.farmshop.repository.ProductRepository;
import com.zhouri.farmshop.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderServiceReceiptFlowTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void confirmReceiptMarksDeliveredOrderAsReceived() {
        Order order = buildOrder(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order saved = orderService.confirmReceipt(10L, 1L);

        assertEquals(OrderStatus.RECEIVED, saved.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void confirmReceiptRejectsNonDeliveredOrder() {
        Order order = buildOrder(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.confirmReceipt(10L, 1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private Order buildOrder(OrderStatus status) {
        return Order.builder()
                .id(1L)
                .code("FS_TEST_001")
                .user(User.builder()
                        .id(10L)
                        .username("consumer")
                        .fullName("普通消费者")
                        .email("consumer@farmshop.local")
                        .role(Role.CONSUMER)
                        .createdAt(LocalDateTime.now())
                        .build())
                .recipientName("普通消费者")
                .recipientPhone("13800000000")
                .recipientAddress("杭州")
                .status(status)
                .paymentStatus(com.zhouri.farmshop.domain.PaymentStatus.PAID)
                .totalAmount(java.math.BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deliveredAt(LocalDateTime.now())
                .build();
    }
}
