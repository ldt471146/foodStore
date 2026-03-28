package com.zhouri.farmshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.PaymentStatus;
import com.zhouri.farmshop.domain.Product;
import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.repository.InventoryMovementRepository;
import com.zhouri.farmshop.repository.OrderRepository;
import com.zhouri.farmshop.repository.ProductRepository;
import com.zhouri.farmshop.repository.ReviewRepository;
import com.zhouri.farmshop.repository.UserRepository;
import java.math.BigDecimal;
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
class AdminServiceOrderStatusFlowTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void updateOrderStatusRejectsDeliveredWithoutLogistics() {
        Order order = buildOrder();
        order.setStatus(OrderStatus.SHIPPED);
        order.setLogisticsCompany(null);
        order.setTrackingNumber(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.updateOrderStatus(2L, Role.FARM_ADMIN, 1L, OrderStatus.DELIVERED)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateOrderStatusMarksDeliveredTime() {
        Order order = buildOrder();
        order.setStatus(OrderStatus.SHIPPED);
        order.setLogisticsCompany("顺丰速运");
        order.setTrackingNumber("SF202603270001");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order saved = adminService.updateOrderStatus(2L, Role.FARM_ADMIN, 1L, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, saved.getStatus());
        assertNotNull(saved.getDeliveredAt());
        verify(orderRepository).save(order);
    }

    @Test
    void updateLogisticsDoesNotRollbackDeliveredOrderToShipped() {
        Order order = buildOrder();
        order.setStatus(OrderStatus.DELIVERED);
        order.setLogisticsCompany("旧物流");
        order.setTrackingNumber("OLD001");
        order.setDeliveredAt(LocalDateTime.now());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order saved = adminService.updateLogistics(2L, Role.FARM_ADMIN, 1L, new AdminService.LogisticsCommand("顺丰速运", "SF202603270001"));

        assertEquals(OrderStatus.DELIVERED, saved.getStatus());
        assertEquals("顺丰速运", saved.getLogisticsCompany());
        assertEquals("SF202603270001", saved.getTrackingNumber());
        verify(orderRepository).save(order);
    }

    @Test
    void farmAdminCannotUpdateOtherFarmOrder() {
        Order order = buildOrder();
        Product otherFarmProduct = Product.builder()
                .id(9L)
                .name("其他农场商品")
                .createdBy(User.builder().id(99L).role(Role.FARM_ADMIN).build())
                .build();
        order.setItems(java.util.List.of(OrderItem.builder().id(1L).order(order).product(otherFarmProduct).build()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.updateLogistics(2L, Role.FARM_ADMIN, 1L, new AdminService.LogisticsCommand("顺丰", "SF1"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private Order buildOrder() {
        Product ownProduct = Product.builder()
                .id(1L)
                .name("晨露羽衣甘蓝")
                .createdBy(User.builder()
                        .id(2L)
                        .username("farmadmin")
                        .fullName("周农场主")
                        .email("farmadmin@farmshop.local")
                        .role(Role.FARM_ADMIN)
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();
        return Order.builder()
                .id(1L)
                .code("FS_TEST_002")
                .user(User.builder()
                        .id(2L)
                        .username("farmadmin")
                        .fullName("周农场主")
                        .email("farmadmin@farmshop.local")
                        .role(Role.FARM_ADMIN)
                        .createdAt(LocalDateTime.now())
                        .build())
                .recipientName("普通消费者")
                .recipientPhone("13800000000")
                .recipientAddress("杭州")
                .status(OrderStatus.PROCESSING)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(java.util.List.of(OrderItem.builder()
                        .id(1L)
                        .product(ownProduct)
                        .productName("晨露羽衣甘蓝")
                        .quantity(1)
                        .subtotal(BigDecimal.ONE)
                        .build()))
                .build();
    }
}
