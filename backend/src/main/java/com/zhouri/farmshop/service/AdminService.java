package com.zhouri.farmshop.service;

import com.zhouri.farmshop.domain.InventoryMovement;
import com.zhouri.farmshop.domain.InventoryMovementType;
import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.PaymentStatus;
import com.zhouri.farmshop.domain.Product;
import com.zhouri.farmshop.domain.Review;
import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.repository.InventoryMovementRepository;
import com.zhouri.farmshop.repository.OrderRepository;
import com.zhouri.farmshop.repository.ProductRepository;
import com.zhouri.farmshop.repository.ReviewRepository;
import com.zhouri.farmshop.repository.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final Path productImageUploadDir = Path.of("uploads", "products").toAbsolutePath().normalize();
    private final Path bundledProductImageDir = Path.of("backend", "uploads", "products").toAbsolutePath().normalize();

    @Transactional(readOnly = true)
    public DashboardData getDashboardData(Long userId, Role role) {
        var managedProducts = listManagedProducts(userId, role);
        var managedProductIds = managedProducts.stream().map(Product::getId).collect(java.util.stream.Collectors.toSet());
        var orders = filterManagedOrders(userId, role, false);
        var paidOrders = orders.stream().filter(order -> order.getPaymentStatus() == PaymentStatus.PAID).toList();
        BigDecimal totalRevenue = paidOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> managedProductIds.contains(item.getProduct().getId()))
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingOrders = orders.stream().filter(order -> order.getStatus() == OrderStatus.PENDING_PAYMENT).count();
        var lowStockProducts = managedProducts.stream()
                .filter(product -> product.getStockQuantity() <= product.getLowStockThreshold())
                .sorted(Comparator.comparing(Product::getStockQuantity))
                .limit(6)
                .toList();

        Map<Long, TopProduct> topProductMap = new LinkedHashMap<>();
        paidOrders.forEach(order -> order.getItems().forEach(item ->
                {
                    if (!managedProductIds.contains(item.getProduct().getId())) {
                        return;
                    }
                    topProductMap.compute(item.getProduct().getId(), (ignored, existing) -> existing == null
                        ? new TopProduct(
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getProduct().getCategory(),
                        item.getQuantity(),
                        item.getSubtotal()
                )
                        : new TopProduct(
                        existing.productId(),
                        existing.name(),
                        existing.category(),
                        existing.soldQuantity() + item.getQuantity(),
                        existing.sales().add(item.getSubtotal())
                ));
                }
        ));

        var topProducts = topProductMap.values().stream()
                .sorted(Comparator.comparing(TopProduct::soldQuantity).reversed())
                .limit(5)
                .toList();

        return new DashboardData(
                totalRevenue,
                orders.size(),
                orders.stream().map(order -> order.getUser().getId()).distinct().count(),
                pendingOrders,
                topProducts,
                lowStockProducts
        );
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts(Long userId, Role role) {
        return listManagedProducts(userId, role).stream()
                .sorted(Comparator.comparing(Product::getFeatured).reversed().thenComparing(Product::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public Product saveProduct(Long userId, Role role, ProductCommand command, Long productId, MultipartFile imageFile) {
        Product product = productId == null
                ? Product.builder().build()
                : productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (productId != null) {
            ensureCanManageProduct(userId, role, product);
        }
        if (product.getCreatedBy() == null) {
            product.setCreatedBy(userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")));
        }
        product.setName(command.name().trim());
        product.setCategory(command.category().trim());
        product.setPrice(command.price());
        product.setStockQuantity(command.stockQuantity());
        product.setUnit(command.unit().trim());
        product.setDescription(command.description().trim());
        product.setFarmName(normalizeNullableText(command.farmName()));
        product.setOrigin(normalizeNullableText(command.origin()));
        product.setCertificate(normalizeNullableText(command.certificate()));
        product.setTraceabilityCode(normalizeNullableText(command.traceabilityCode()));
        product.setPlantingDate(command.plantingDate());
        product.setHarvestDate(command.harvestDate());
        product.setOrganic(command.organic());
        product.setFeatured(command.featured());
        product.setLowStockThreshold(command.lowStockThreshold());
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageUrl(storeProductImage(imageFile));
        }
        if (product.getRating() == null) {
            product.setRating(0.0);
        }
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductReviewView> listProductReviews(Long userId, Role role) {
        return productRepository.findAll().stream()
                .filter(product -> role == Role.PLATFORM_ADMIN || product.getCreatedBy().getId().equals(userId))
                .flatMap(product -> reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId()).stream()
                        .map(review -> new ProductReviewView(
                                product.getId(),
                                product.getName(),
                                review.getId(),
                                review.getUser().getFullName(),
                                review.getRating(),
                                review.getContent(),
                                review.getCreatedAt()
                        )))
                .sorted(Comparator.comparing(ProductReviewView::createdAt).reversed())
                .toList();
    }

    public byte[] readProductImage(String filename) {
        Path filePath = resolveProductImage(filename);
        if (filePath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品图片不存在");
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取商品图片失败");
        }
    }

    @Transactional
    public void deleteProduct(Long userId, Role role, Long productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        ensureCanManageProduct(userId, role, product);
        productRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> listInventoryMovements(Long userId, Role role) {
        return inventoryMovementRepository.findAll().stream()
                .filter(movement -> role == Role.PLATFORM_ADMIN
                        || movement.getProduct().getCreatedBy().getId().equals(userId))
                .sorted(Comparator.comparing(InventoryMovement::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public InventoryMovement createInventoryMovement(Long userId, Role role, InventoryCommand command) {
        var product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        ensureCanManageProduct(userId, role, product);
        if (command.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存变动数量必须大于 0");
        }

        int delta = switch (command.type()) {
            case INBOUND -> command.quantity();
            case OUTBOUND -> -command.quantity();
            case ADJUSTMENT -> command.quantity();
        };

        int nextStock = product.getStockQuantity() + delta;
        if (nextStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存不足");
        }
        product.setStockQuantity(nextStock);
        productRepository.save(product);
        return inventoryMovementRepository.save(InventoryMovement.builder()
                .product(product)
                .type(command.type())
                .quantity(command.quantity())
                .source(command.source())
                .remark(command.remark())
                .build());
    }

    @Transactional(readOnly = true)
    public List<Order> listOrders(Long userId, Role role) {
        return filterManagedOrders(userId, role, false).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public Order updateOrderStatus(Long userId, Role role, Long orderId, OrderStatus status) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        ensureCanManageOrder(userId, role, order, true);
        if (order.getStatus() == OrderStatus.RECEIVED && status != OrderStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户已确认收货，订单状态不能再由后台修改");
        }
        if (status == OrderStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已收货只能由消费者确认");
        }
        if (status == OrderStatus.SHIPPED
                && (!StringUtils.hasText(order.getLogisticsCompany()) || !StringUtils.hasText(order.getTrackingNumber()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先保存物流信息，再标记为已发货");
        }
        if (status == OrderStatus.DELIVERED) {
            if (order.getStatus() != OrderStatus.SHIPPED && order.getStatus() != OrderStatus.DELIVERED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已发货订单才能标记为已送达");
            }
            if (!StringUtils.hasText(order.getLogisticsCompany()) || !StringUtils.hasText(order.getTrackingNumber())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先填写物流信息，再标记为已送达");
            }
        }
        order.setStatus(status);
        if (status == OrderStatus.SHIPPED && order.getShippedAt() == null) {
            order.setShippedAt(LocalDateTime.now());
        }
        if (status == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateLogistics(Long userId, Role role, Long orderId, LogisticsCommand command) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        ensureCanManageOrder(userId, role, order, true);
        if (order.getStatus() == OrderStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户已确认收货，不能再修改物流信息");
        }
        order.setLogisticsCompany(command.company().trim());
        order.setTrackingNumber(command.trackingNumber().trim());
        if (order.getStatus() != OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.SHIPPED);
        }
        if (order.getShippedAt() == null) {
            order.setShippedAt(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<CustomerProfile> getCustomerProfiles(Long userId, Role role) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole().name().contains("CONSUMER"))
                .map(user -> buildCustomerProfile(user, userId, role))
                .filter(profile -> role == Role.PLATFORM_ADMIN || profile.paidOrderCount() > 0)
                .sorted(Comparator.comparing(CustomerProfile::totalSpent).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ForecastData getForecastData(Long userId, Role role) {
        Map<YearMonth, Integer> monthlySales = new LinkedHashMap<>();
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        var managedProductIds = listManagedProducts(userId, role).stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (int i = 5; i >= 0; i--) {
            monthlySales.put(YearMonth.now().minusMonths(i), 0);
        }

        filterManagedOrders(userId, role, false).stream()
                .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                .forEach(order -> {
                    YearMonth key = YearMonth.from(order.getPaidAt() == null ? order.getCreatedAt() : order.getPaidAt());
                    int sold = order.getItems().stream()
                            .filter(item -> managedProductIds.contains(item.getProduct().getId()))
                            .mapToInt(item -> item.getQuantity())
                            .sum();
                    monthlySales.computeIfPresent(key, (ignored, existing) -> existing + sold);
                });

        List<String> labels = monthlySales.keySet().stream().map(month -> month.format(formatter)).toList();
        List<Integer> history = new ArrayList<>(monthlySales.values());
        double slope = history.size() < 2 ? 0 : (history.get(history.size() - 1) - history.get(0)) / (double) (history.size() - 1);
        double movingAverage = history.stream().mapToInt(Integer::intValue).average().orElse(0);

        List<String> forecastLabels = new ArrayList<>();
        List<Integer> forecastValues = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            YearMonth future = YearMonth.now().plusMonths(i);
            forecastLabels.add(future.format(formatter));
            forecastValues.add((int) Math.max(0, Math.round(movingAverage + slope * i)));
        }
        return new ForecastData(labels, history, forecastLabels, forecastValues);
    }

    private CustomerProfile buildCustomerProfile(User user, Long adminUserId, Role role) {
        var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(order -> role == Role.PLATFORM_ADMIN || orderContainsManagedProduct(order, adminUserId))
                .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                .toList();
        BigDecimal totalSpent = orders.stream()
                .map(order -> orderSubtotalForFarmAdmin(order, adminUserId, role))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Integer> categories = new LinkedHashMap<>();
        orders.forEach(order -> order.getItems().forEach(item ->
                {
                    if (role == Role.PLATFORM_ADMIN || item.getProduct().getCreatedBy().getId().equals(adminUserId)) {
                        categories.merge(item.getProduct().getCategory(), item.getQuantity(), Integer::sum);
                    }
                }
        ));
        String favoriteCategory = categories.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无");
        LocalDate lastOrderDate = orders.isEmpty() ? null : orders.get(0).getCreatedAt().toLocalDate();
        return new CustomerProfile(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                orders.size(),
                totalSpent,
                favoriteCategory,
                lastOrderDate
        );
    }

    private String storeProductImage(MultipartFile imageFile) {
        String originalFilename = imageFile.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品图片仅支持 jpg、jpeg、png、webp");
        }
        try {
            Files.createDirectories(productImageUploadDir);
            String filename = UUID.randomUUID() + extension;
            Files.copy(imageFile.getInputStream(), productImageUploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/api/admin/product-images/" + filename;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存商品图片失败");
        }
    }

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Path resolveProductImage(String filename) {
        Path uploadedPath = productImageUploadDir.resolve(filename).toAbsolutePath().normalize();
        if (uploadedPath.startsWith(productImageUploadDir) && Files.exists(uploadedPath)) {
            return uploadedPath;
        }
        Path bundledPath = bundledProductImageDir.resolve(filename).toAbsolutePath().normalize();
        if (bundledPath.startsWith(bundledProductImageDir) && Files.exists(bundledPath)) {
            return bundledPath;
        }
        return null;
    }

    private void ensureCanManageProduct(Long userId, Role role, Product product) {
        if (role == Role.PLATFORM_ADMIN) {
            return;
        }
        if (!product.getCreatedBy().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作其他人发布的商品");
        }
    }

    private void ensureCanManageOrder(Long userId, Role role, Order order, boolean exclusive) {
        if (role == Role.PLATFORM_ADMIN) {
            return;
        }
        boolean ownsAnyItem = orderContainsManagedProduct(order, userId);
        if (!ownsAnyItem) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作其他农场的订单");
        }
        if (exclusive) {
            boolean ownsAllItems = order.getItems().stream()
                    .allMatch(item -> item.getProduct().getCreatedBy().getId().equals(userId));
            if (!ownsAllItems) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "订单包含其他农场商品，不能由当前农场主统一发货");
            }
        }
    }

    private List<Product> listManagedProducts(Long userId, Role role) {
        return productRepository.findAll().stream()
                .filter(product -> role == Role.PLATFORM_ADMIN || product.getCreatedBy().getId().equals(userId))
                .toList();
    }

    private List<Order> filterManagedOrders(Long userId, Role role, boolean exclusive) {
        return orderRepository.findAll().stream()
                .filter(order -> role == Role.PLATFORM_ADMIN || (exclusive
                        ? order.getItems().stream().allMatch(item -> item.getProduct().getCreatedBy().getId().equals(userId))
                        : orderContainsManagedProduct(order, userId)))
                .toList();
    }

    private boolean orderContainsManagedProduct(Order order, Long userId) {
        return order.getItems().stream()
                .anyMatch(item -> item.getProduct().getCreatedBy().getId().equals(userId));
    }

    private BigDecimal orderSubtotalForFarmAdmin(Order order, Long userId, Role role) {
        if (role == Role.PLATFORM_ADMIN) {
            return order.getTotalAmount();
        }
        return order.getItems().stream()
                .filter(item -> item.getProduct().getCreatedBy().getId().equals(userId))
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record DashboardData(
            BigDecimal totalRevenue,
            long totalOrders,
            long totalCustomers,
            long pendingOrders,
            List<TopProduct> topProducts,
            List<Product> lowStockProducts
    ) {
    }

    public record TopProduct(Long productId, String name, String category, int soldQuantity, BigDecimal sales) {
    }

    public record ProductCommand(
            String name,
            String category,
            BigDecimal price,
            Integer stockQuantity,
            String unit,
            String description,
            String farmName,
            String origin,
            String certificate,
            String traceabilityCode,
            LocalDate plantingDate,
            LocalDate harvestDate,
            Boolean organic,
            Boolean featured,
            Integer lowStockThreshold
    ) {
    }

    public record InventoryCommand(
            Long productId,
            InventoryMovementType type,
            Integer quantity,
            String source,
            String remark
    ) {
    }

    public record LogisticsCommand(String company, String trackingNumber) {
    }

    public record CustomerProfile(
            Long id,
            String fullName,
            String username,
            String email,
            String phone,
            int paidOrderCount,
            BigDecimal totalSpent,
            String favoriteCategory,
            LocalDate lastOrderDate
    ) {
    }

    public record ForecastData(
            List<String> historyLabels,
            List<Integer> historyValues,
            List<String> forecastLabels,
            List<Integer> forecastValues
    ) {
    }

    public record ProductReviewView(
            Long productId,
            String productName,
            Long reviewId,
            String customerName,
            Integer rating,
            String content,
            LocalDateTime createdAt
    ) {
    }
}
