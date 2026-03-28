package com.zhouri.farmshop.controller;

import com.zhouri.farmshop.domain.InventoryMovement;
import com.zhouri.farmshop.domain.InventoryMovementType;
import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import com.zhouri.farmshop.domain.OrderStatus;
import com.zhouri.farmshop.domain.Product;
import com.zhouri.farmshop.security.AuthenticatedUser;
import com.zhouri.farmshop.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/overview")
    public DashboardResponse overview(Authentication authentication) {
        var user = currentUser(authentication);
        var dashboard = adminService.getDashboardData(user.id(), user.role());
        return new DashboardResponse(
                dashboard.totalRevenue(),
                dashboard.totalOrders(),
                dashboard.totalCustomers(),
                dashboard.pendingOrders(),
                dashboard.topProducts(),
                dashboard.lowStockProducts().stream().map(this::toProductResponse).toList()
        );
    }

    @GetMapping("/products")
    public List<ProductResponse> products(Authentication authentication) {
        var user = currentUser(authentication);
        return adminService.listProducts(user.id(), user.role()).stream().map(this::toProductResponse).toList();
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse createProduct(
            Authentication authentication,
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String category,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal price,
            @RequestParam @NotNull @Min(0) Integer stockQuantity,
            @RequestParam @NotBlank String unit,
            @RequestParam @NotBlank String description,
            @RequestParam(required = false) String farmName,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String certificate,
            @RequestParam(required = false) String traceabilityCode,
            @RequestParam(required = false) LocalDate plantingDate,
            @RequestParam(required = false) LocalDate harvestDate,
            @RequestParam @NotNull Boolean organic,
            @RequestParam @NotNull Boolean featured,
            @RequestParam @NotNull @Min(1) Integer lowStockThreshold,
            @RequestParam(required = false) MultipartFile imageFile
    ) {
        var user = currentUser(authentication);
        return toProductResponse(adminService.saveProduct(
                user.id(),
                user.role(),
                new AdminService.ProductCommand(
                        name,
                        category,
                        price,
                        stockQuantity,
                        unit,
                        description,
                        farmName,
                        origin,
                        certificate,
                        traceabilityCode,
                        plantingDate,
                        harvestDate,
                        organic,
                        featured,
                        lowStockThreshold
                ),
                null,
                imageFile
        ));
    }

    @PutMapping(value = "/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse updateProduct(
            @PathVariable Long productId,
            Authentication authentication,
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String category,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal price,
            @RequestParam @NotNull @Min(0) Integer stockQuantity,
            @RequestParam @NotBlank String unit,
            @RequestParam @NotBlank String description,
            @RequestParam(required = false) String farmName,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String certificate,
            @RequestParam(required = false) String traceabilityCode,
            @RequestParam(required = false) LocalDate plantingDate,
            @RequestParam(required = false) LocalDate harvestDate,
            @RequestParam @NotNull Boolean organic,
            @RequestParam @NotNull Boolean featured,
            @RequestParam @NotNull @Min(1) Integer lowStockThreshold,
            @RequestParam(required = false) MultipartFile imageFile
    ) {
        var user = currentUser(authentication);
        return toProductResponse(adminService.saveProduct(
                user.id(),
                user.role(),
                new AdminService.ProductCommand(
                        name,
                        category,
                        price,
                        stockQuantity,
                        unit,
                        description,
                        farmName,
                        origin,
                        certificate,
                        traceabilityCode,
                        plantingDate,
                        harvestDate,
                        organic,
                        featured,
                        lowStockThreshold
                ),
                productId,
                imageFile
        ));
    }

    @DeleteMapping("/products/{productId}")
    public void deleteProduct(@PathVariable Long productId, Authentication authentication) {
        var user = currentUser(authentication);
        adminService.deleteProduct(user.id(), user.role(), productId);
    }

    @GetMapping("/product-images/{filename}")
    public ResponseEntity<byte[]> productImage(@PathVariable String filename) {
        byte[] file = adminService.readProductImage(filename);
        String contentType = filename.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE
                : filename.endsWith(".webp") ? "image/webp"
                : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(file);
    }

    @GetMapping("/inventory")
    public List<InventoryResponse> inventory(Authentication authentication) {
        var user = currentUser(authentication);
        return adminService.listInventoryMovements(user.id(), user.role()).stream().map(this::toInventoryResponse).toList();
    }

    @GetMapping("/product-reviews")
    public List<ProductReviewResponse> productReviews(Authentication authentication) {
        var user = currentUser(authentication);
        return adminService.listProductReviews(user.id(), user.role()).stream()
                .map(review -> new ProductReviewResponse(
                        review.productId(),
                        review.productName(),
                        review.reviewId(),
                        review.customerName(),
                        review.rating(),
                        review.content(),
                        review.createdAt()
                ))
                .toList();
    }

    @PostMapping("/inventory/movements")
    public InventoryResponse createInventory(@Valid @RequestBody InventoryRequest request, Authentication authentication) {
        var user = currentUser(authentication);
        return toInventoryResponse(adminService.createInventoryMovement(user.id(), user.role(), new AdminService.InventoryCommand(
                request.productId(),
                request.type(),
                request.quantity(),
                request.source(),
                request.remark()
        )));
    }

    @GetMapping("/orders")
    public List<AdminOrderResponse> orders(Authentication authentication) {
        var user = currentUser(authentication);
        return adminService.listOrders(user.id(), user.role()).stream().map(this::toAdminOrderResponse).toList();
    }

    @PatchMapping("/orders/{orderId}/status")
    public AdminOrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication
    ) {
        var user = currentUser(authentication);
        return toAdminOrderResponse(adminService.updateOrderStatus(user.id(), user.role(), orderId, request.status()));
    }

    @PatchMapping("/orders/{orderId}/logistics")
    public AdminOrderResponse updateLogistics(
            @PathVariable Long orderId,
            @Valid @RequestBody LogisticsRequest request,
            Authentication authentication
    ) {
        var user = currentUser(authentication);
        return toAdminOrderResponse(adminService.updateLogistics(user.id(), user.role(), orderId, new AdminService.LogisticsCommand(
                request.company(),
                request.trackingNumber()
        )));
    }

    @GetMapping("/customers")
    public List<AdminService.CustomerProfile> customers(Authentication authentication) {
        var user = currentUser(authentication);
        return adminService.getCustomerProfiles(user.id(), user.role());
    }

    @GetMapping("/analytics/forecast")
    public AdminService.ForecastData forecast(Authentication authentication) {
        var user = currentUser(authentication);
        return adminService.getForecastData(user.id(), user.role());
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getUnit(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCreatedBy().getFullName(),
                product.getFarmName(),
                product.getOrigin(),
                product.getCertificate(),
                product.getTraceabilityCode(),
                product.getPlantingDate(),
                product.getHarvestDate(),
                product.getOrganic(),
                product.getFeatured(),
                product.getRating(),
                product.getLowStockThreshold()
        );
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    private InventoryResponse toInventoryResponse(InventoryMovement movement) {
        return new InventoryResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getType(),
                movement.getQuantity(),
                movement.getSource(),
                movement.getRemark(),
                movement.getCreatedAt()
        );
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        return new AdminOrderResponse(
                order.getId(),
                order.getCode(),
                order.getUser().getFullName(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getRecipientAddress(),
                order.getLogisticsCompany(),
                order.getTrackingNumber(),
            order.getCreatedAt(),
            order.getStatus() == OrderStatus.RECEIVED ? order.getUpdatedAt() : null,
            order.getItems().stream().map(this::toOrderItem).toList()
        );
    }

    private OrderItemResponse toOrderItem(OrderItem item) {
        return new OrderItemResponse(
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    public record DashboardResponse(
            BigDecimal totalRevenue,
            long totalOrders,
            long totalCustomers,
            long pendingOrders,
            List<AdminService.TopProduct> topProducts,
            List<ProductResponse> lowStockProducts
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String category,
            BigDecimal price,
            Integer stockQuantity,
            String unit,
            String description,
            String imageUrl,
            String publisherName,
            String farmName,
            String origin,
            String certificate,
            String traceabilityCode,
            LocalDate plantingDate,
            LocalDate harvestDate,
            Boolean organic,
            Boolean featured,
            Double rating,
            Integer lowStockThreshold
    ) {
    }

    public record ProductReviewResponse(
            Long productId,
            String productName,
            Long reviewId,
            String customerName,
            Integer rating,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record InventoryRequest(
            @NotNull Long productId,
            @NotNull InventoryMovementType type,
            @NotNull @Min(1) Integer quantity,
            String source,
            String remark
    ) {
    }

    public record InventoryResponse(
            Long id,
            Long productId,
            String productName,
            InventoryMovementType type,
            Integer quantity,
            String source,
            String remark,
            LocalDateTime createdAt
    ) {
    }

    public record UpdateStatusRequest(@NotNull OrderStatus status) {
    }

    public record LogisticsRequest(@NotBlank String company, @NotBlank String trackingNumber) {
    }

    public record OrderItemResponse(String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public record AdminOrderResponse(
            Long id,
            String code,
            String customerName,
            String username,
            com.zhouri.farmshop.domain.OrderStatus status,
            com.zhouri.farmshop.domain.PaymentStatus paymentStatus,
            BigDecimal totalAmount,
            String recipientName,
            String recipientPhone,
            String recipientAddress,
            String logisticsCompany,
            String trackingNumber,
            LocalDateTime createdAt,
            LocalDateTime receivedAt,
            List<OrderItemResponse> items
    ) {
    }
}
