package com.zhouri.farmshop.service;

import com.zhouri.farmshop.domain.BrowseRecord;
import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.PaymentStatus;
import com.zhouri.farmshop.domain.Product;
import com.zhouri.farmshop.domain.Review;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.repository.BrowseRecordRepository;
import com.zhouri.farmshop.repository.OrderRepository;
import com.zhouri.farmshop.repository.ProductRepository;
import com.zhouri.farmshop.repository.ReviewRepository;
import com.zhouri.farmshop.repository.UserRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final BrowseRecordRepository browseRecordRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public HomeData getHomeData() {
        var products = productRepository.findAll();
        var featured = products.stream()
                .filter(product -> Boolean.TRUE.equals(product.getFeatured()))
                .sorted(Comparator.comparing(Product::getRating).reversed())
                .limit(4)
                .toList();
        if (featured.isEmpty()) {
            featured = products.stream()
                    .sorted(Comparator.comparing(Product::getRating).reversed())
                    .limit(4)
                    .toList();
        }
        var editorChoice = products.stream()
                .sorted(Comparator.comparing(Product::getStockQuantity).thenComparing(Product::getRating).reversed())
                .limit(4)
                .toList();
        var categories = products.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        return new HomeData(featured, editorChoice, categories);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String category, String query, Boolean featuredOnly) {
        var normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return productRepository.findAll().stream()
                .filter(product -> category == null || category.isBlank()
                        || product.getCategory().equalsIgnoreCase(category))
                .filter(product -> !Boolean.TRUE.equals(featuredOnly) || Boolean.TRUE.equals(product.getFeatured()))
                .filter(product -> normalizedQuery.isBlank()
                        || product.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || product.getDescription().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(Product::getFeatured).reversed()
                        .thenComparing(Product::getRating).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return productRepository.findAll().stream()
                .map(Product::getCategory)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public void recordBrowse(Long userId, Long productId) {
        var user = getUser(userId);
        var product = getProduct(productId);
        browseRecordRepository.save(BrowseRecord.builder()
                .user(user)
                .product(product)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Product> getRecommendations(Long userId) {
        if (userId == null) {
            return productRepository.findByFeaturedTrueOrderByRatingDesc().stream().limit(6).toList();
        }

        Map<String, Integer> scoresByCategory = new HashMap<>();
        Set<Long> purchasedProductIds = new java.util.HashSet<>();

        browseRecordRepository.findTop20ByUserIdOrderByViewedAtDesc(userId).forEach(record ->
                scoresByCategory.merge(record.getProduct().getCategory(), 1, Integer::sum)
        );

        for (Order order : orderRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            order.getItems().forEach(item -> {
                purchasedProductIds.add(item.getProduct().getId());
                scoresByCategory.merge(item.getProduct().getCategory(), 3, Integer::sum);
            });
        }

        return productRepository.findAll().stream()
                .sorted((left, right) -> Double.compare(score(right, scoresByCategory, purchasedProductIds),
                        score(left, scoresByCategory, purchasedProductIds)))
                .limit(6)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Review> getReviews(Long productId) {
        getProduct(productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public ReviewEligibility getProductReviewEligibility(Long userId, Long productId) {
        getProduct(productId);
        Long orderItemId = findLatestReviewableOrderItemId(userId, productId);
        if (orderItemId == null) {
            return new ReviewEligibility(false, false, "确认收货后才能评论", null);
        }
        return new ReviewEligibility(true, false, "可以继续评论", orderItemId);
    }

    @Transactional(readOnly = true)
    public ReviewEligibility getOrderItemReviewEligibility(Long userId, OrderItem orderItem) {
        if (!orderItem.getOrder().getUser().getId().equals(userId)) {
            return new ReviewEligibility(false, false, "无权评论该订单商品", null);
        }
        if (orderItem.getOrder().getStatus() != OrderStatus.RECEIVED) {
            return new ReviewEligibility(false, false, "确认收货后才能评论", null);
        }
        return new ReviewEligibility(true, false, "可以继续评论", orderItem.getId());
    }

    @Transactional
    public Review addReview(Long userId, Long productId, Long orderItemId, int rating, String content) {
        var user = getUser(userId);
        var product = getProduct(productId);
        var eligibility = validateReviewableOrderItem(userId, productId, orderItemId);
        if (!eligibility.canReview()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, eligibility.message());
        }

        var review = reviewRepository.save(Review.builder()
                .user(user)
                .product(product)
                .rating(rating)
                .content(content.trim())
                .build());

        double average = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);
        product.setRating(Math.round(average * 10.0) / 10.0);
        productRepository.save(product);
        return review;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private ReviewEligibility validateReviewableOrderItem(Long userId, Long productId, Long orderItemId) {
        if (orderItemId == null) {
            return new ReviewEligibility(false, false, "请从已确认收货的订单商品发起评论", null);
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .map(item -> {
                    if (!item.getProduct().getId().equals(productId)) {
                        return new ReviewEligibility(false, false, "订单商品与当前评论商品不一致", null);
                    }
                    return getOrderItemReviewEligibility(userId, item);
                })
                .orElseGet(() -> new ReviewEligibility(false, false, "未找到可评论的订单商品", null));
    }

    private Long findLatestReviewableOrderItemId(Long userId, Long productId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(order -> order.getStatus() == OrderStatus.RECEIVED)
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getProduct().getId().equals(productId))
                .map(OrderItem::getId)
                .findFirst()
                .orElse(null);
    }

    private double score(Product product, Map<String, Integer> scoresByCategory, Set<Long> purchasedProductIds) {
        int categoryWeight = scoresByCategory.getOrDefault(product.getCategory(), 0);
        int purchasedPenalty = purchasedProductIds.contains(product.getId()) ? -2 : 0;
        int featuredBonus = Boolean.TRUE.equals(product.getFeatured()) ? 2 : 0;
        int organicBonus = Boolean.TRUE.equals(product.getOrganic()) ? 1 : 0;
        return categoryWeight * 10 + product.getRating() * 2 + featuredBonus + organicBonus + purchasedPenalty;
    }

    public record HomeData(List<Product> featuredProducts, List<Product> editorChoice, List<String> categories) {
    }

    public record ReviewEligibility(boolean canReview, boolean reviewed, String message, Long orderItemId) {
    }
}
