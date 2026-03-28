package com.zhouri.farmshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.PaymentStatus;
import com.zhouri.farmshop.domain.Product;
import com.zhouri.farmshop.domain.Review;
import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.repository.BrowseRecordRepository;
import com.zhouri.farmshop.repository.OrderRepository;
import com.zhouri.farmshop.repository.ProductRepository;
import com.zhouri.farmshop.repository.ReviewRepository;
import com.zhouri.farmshop.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceMultiReviewTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BrowseRecordRepository browseRecordRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void reviewEligibilityStillAllowsCommentWhenUserHasReviewedBefore() {
        Product product = buildProduct();
        Order receivedOrder = buildReceivedOrder(product);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(receivedOrder));

        CatalogService.ReviewEligibility eligibility = catalogService.getProductReviewEligibility(1L, 1L);

        assertTrue(eligibility.canReview());
        assertFalse(eligibility.reviewed());
        assertEquals("可以继续评论", eligibility.message());
        assertEquals(1L, eligibility.orderItemId());
    }

    @Test
    void orderItemWithoutReceiptCannotReview() {
        Product product = buildProduct();
        Order deliveredOrder = buildDeliveredOrder(product);
        OrderItem deliveredItem = deliveredOrder.getItems().get(0);

        CatalogService.ReviewEligibility eligibility = catalogService.getOrderItemReviewEligibility(1L, deliveredItem);

        assertFalse(eligibility.canReview());
        assertEquals("确认收货后才能评论", eligibility.message());
    }

    @Test
    void addReviewAllowsMultipleCommentsFromSameUser() {
        User consumer = buildUser();
        Product product = buildProduct();
        Review firstReview = Review.builder()
                .id(1L)
                .user(consumer)
                .product(product)
                .rating(5)
                .content("第一次评价")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        Order receivedOrder = buildReceivedOrder(product);

        when(userRepository.findById(1L)).thenReturn(Optional.of(consumer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(receivedOrder));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(firstReview, firstReview));

        Review saved = catalogService.addReview(1L, 1L, 1L, 4, "第二次评价");

        assertEquals("第二次评价", saved.getContent());
        verify(reviewRepository).save(any(Review.class));
        verify(productRepository).save(product);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .username("consumer")
                .fullName("普通消费者")
                .email("consumer@farmshop.local")
                .role(Role.CONSUMER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Product buildProduct() {
        return Product.builder()
                .id(1L)
                .name("晨露羽衣甘蓝")
                .category("蔬菜")
                .price(BigDecimal.TEN)
                .stockQuantity(10)
                .unit("把")
                .description("测试商品")
                .createdBy(User.builder()
                        .id(2L)
                        .username("farmadmin")
                        .fullName("周农场主")
                        .email("farmadmin@farmshop.local")
                        .role(Role.FARM_ADMIN)
                        .createdAt(LocalDateTime.now())
                        .build())
                .rating(4.0)
                .lowStockThreshold(2)
                .organic(true)
                .featured(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Order buildReceivedOrder(Product product) {
        return buildOrder(product, OrderStatus.RECEIVED, 1L);
    }

    private Order buildDeliveredOrder(Product product) {
        return buildOrder(product, OrderStatus.DELIVERED, 2L);
    }

    private Order buildOrder(Product product, OrderStatus status, Long itemId) {
        User consumer = buildUser();
        Order order = Order.builder()
                .id(1L)
                .code("FS_TEST_REVIEW")
                .user(consumer)
                .recipientName("普通消费者")
                .recipientPhone("13800000000")
                .recipientAddress("杭州")
                .status(status)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order.setItems(List.of(OrderItem.builder()
                .id(itemId)
                .order(order)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(1)
                .subtotal(product.getPrice())
                .build()));
        return order;
    }
}
