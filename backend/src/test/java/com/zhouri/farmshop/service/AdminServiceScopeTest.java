package com.zhouri.farmshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.zhouri.farmshop.domain.InventoryMovement;
import com.zhouri.farmshop.domain.InventoryMovementType;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceScopeTest {

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
    void farmAdminDashboardUsesOnlyOwnProductsAndOrders() {
        User farmAdmin = buildFarmAdmin(2L);
        User otherFarmAdmin = buildFarmAdmin(3L);
        User consumer = User.builder().id(10L).role(Role.CONSUMER).fullName("消费者").username("consumer").build();
        Product ownProduct = buildProduct(1L, farmAdmin, "自家菜");
        Product otherProduct = buildProduct(2L, otherFarmAdmin, "别家菜");
        Order ownOrder = buildOrder(100L, consumer, ownProduct, BigDecimal.valueOf(88));
        Order otherOrder = buildOrder(101L, consumer, otherProduct, BigDecimal.valueOf(66));

        when(productRepository.findAll()).thenReturn(List.of(ownProduct, otherProduct));
        when(orderRepository.findAll()).thenReturn(List.of(ownOrder, otherOrder));

        AdminService.DashboardData dashboard = adminService.getDashboardData(2L, Role.FARM_ADMIN);

        assertEquals(BigDecimal.valueOf(88), dashboard.totalRevenue());
        assertEquals(1, dashboard.totalOrders());
        assertEquals(1, dashboard.topProducts().size());
        assertEquals(ownProduct.getId(), dashboard.topProducts().get(0).productId());
    }

    @Test
    void farmAdminInventoryListIncludesOnlyOwnProductMovements() {
        User farmAdmin = buildFarmAdmin(2L);
        User otherFarmAdmin = buildFarmAdmin(3L);
        Product ownProduct = buildProduct(1L, farmAdmin, "自家菜");
        Product otherProduct = buildProduct(2L, otherFarmAdmin, "别家菜");
        InventoryMovement ownMovement = InventoryMovement.builder().id(1L).product(ownProduct).type(InventoryMovementType.INBOUND).quantity(5).createdAt(LocalDateTime.now()).build();
        InventoryMovement otherMovement = InventoryMovement.builder().id(2L).product(otherProduct).type(InventoryMovementType.INBOUND).quantity(7).createdAt(LocalDateTime.now()).build();

        when(inventoryMovementRepository.findAll()).thenReturn(List.of(ownMovement, otherMovement));

        List<InventoryMovement> movements = adminService.listInventoryMovements(2L, Role.FARM_ADMIN);

        assertEquals(1, movements.size());
        assertEquals(1L, movements.get(0).getId());
    }

    @Test
    void farmAdminOrdersListIncludesOnlyOwnOrders() {
        User farmAdmin = buildFarmAdmin(2L);
        User otherFarmAdmin = buildFarmAdmin(3L);
        User consumer = User.builder().id(10L).role(Role.CONSUMER).fullName("消费者").username("consumer").build();
        Product ownProduct = buildProduct(1L, farmAdmin, "自家菜");
        Product otherProduct = buildProduct(2L, otherFarmAdmin, "别家菜");
        Order ownOrder = buildOrder(100L, consumer, ownProduct, BigDecimal.valueOf(88));
        Order otherOrder = buildOrder(101L, consumer, otherProduct, BigDecimal.valueOf(66));

        when(orderRepository.findAll()).thenReturn(List.of(ownOrder, otherOrder));

        List<Order> orders = adminService.listOrders(2L, Role.FARM_ADMIN);

        assertEquals(1, orders.size());
        assertEquals(100L, orders.get(0).getId());
    }

    private User buildFarmAdmin(Long id) {
        return User.builder().id(id).role(Role.FARM_ADMIN).username("farm" + id).fullName("农场主" + id).createdAt(LocalDateTime.now()).build();
    }

    private Product buildProduct(Long id, User owner, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .category("蔬菜")
                .price(BigDecimal.TEN)
                .stockQuantity(10)
                .unit("件")
                .description(name)
                .createdBy(owner)
                .lowStockThreshold(3)
                .featured(false)
                .organic(true)
                .rating(4.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Order buildOrder(Long id, User consumer, Product product, BigDecimal subtotal) {
        Order order = Order.builder()
                .id(id)
                .code("FS" + id)
                .user(consumer)
                .recipientName("收货人")
                .recipientPhone("138")
                .recipientAddress("杭州")
                .status(OrderStatus.PROCESSING)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(subtotal)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        OrderItem item = OrderItem.builder()
                .id(id)
                .order(order)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(1)
                .subtotal(subtotal)
                .build();
        order.setItems(List.of(item));
        return order;
    }
}
