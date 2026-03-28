package com.zhouri.farmshop.controller;

import com.zhouri.farmshop.domain.Product;
import com.zhouri.farmshop.domain.Review;
import com.zhouri.farmshop.security.AuthenticatedUser;
import com.zhouri.farmshop.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class StorefrontController {

    private final CatalogService catalogService;

    @GetMapping("/home")
    public HomeResponse home() {
        var home = catalogService.getHomeData();
        return new HomeResponse(
                home.categories(),
                home.featuredProducts().stream().map(this::toProductSummary).toList(),
                home.editorChoice().stream().map(this::toProductSummary).toList()
        );
    }

    @GetMapping("/products")
    public List<ProductSummary> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean featured
    ) {
        return catalogService.searchProducts(category, query, featured).stream()
                .map(this::toProductSummary)
                .toList();
    }

    @GetMapping("/products/{productId}")
    public ProductDetail productDetail(@PathVariable Long productId) {
        return toProductDetail(catalogService.getProduct(productId));
    }

    @GetMapping("/products/{productId}/review-eligibility")
    public ReviewEligibilityResponse reviewEligibility(@PathVariable Long productId, Authentication authentication) {
        var user = (AuthenticatedUser) authentication.getPrincipal();
        var eligibility = catalogService.getProductReviewEligibility(user.id(), productId);
        return new ReviewEligibilityResponse(
                eligibility.canReview(),
                eligibility.reviewed(),
                eligibility.message(),
                eligibility.orderItemId()
        );
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return catalogService.listCategories();
    }

    @PostMapping("/products/{productId}/browse")
    public void browse(@PathVariable Long productId, Authentication authentication) {
        var user = (AuthenticatedUser) authentication.getPrincipal();
        catalogService.recordBrowse(user.id(), productId);
    }

    @GetMapping("/recommendations")
    public List<ProductSummary> recommendations(Authentication authentication) {
        Long userId = authentication == null ? null : ((AuthenticatedUser) authentication.getPrincipal()).id();
        return catalogService.getRecommendations(userId).stream()
                .map(this::toProductSummary)
                .toList();
    }

    @GetMapping("/products/{productId}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long productId) {
        return catalogService.getReviews(productId).stream().map(this::toReviewResponse).toList();
    }

    @PostMapping("/products/{productId}/reviews")
    public ReviewResponse createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        var user = (AuthenticatedUser) authentication.getPrincipal();
        var review = catalogService.addReview(user.id(), productId, request.orderItemId(), request.rating(), request.content());
        return toReviewResponse(review);
    }

    private ProductSummary toProductSummary(Product product) {
        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getRating(),
                product.getImageUrl(),
                product.getCreatedBy().getFullName(),
                product.getOrigin(),
                product.getFarmName(),
                product.getStockQuantity(),
                product.getFeatured(),
                product.getOrganic()
        );
    }

    private ProductDetail toProductDetail(Product product) {
        return new ProductDetail(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getRating(),
                product.getImageUrl(),
                product.getCreatedBy().getFullName(),
                product.getDescription(),
                product.getOrigin(),
                product.getFarmName(),
                product.getTraceabilityCode(),
                product.getCertificate(),
                product.getPlantingDate(),
                product.getHarvestDate(),
                product.getUnit(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.getOrganic(),
                product.getFeatured()
        );
    }

    private ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }

    public record HomeResponse(List<String> categories, List<ProductSummary> featuredProducts, List<ProductSummary> editorChoice) {
    }

    public record ProductSummary(
            Long id,
            String name,
            String category,
            java.math.BigDecimal price,
            Double rating,
            String imageUrl,
            String publisherName,
            String origin,
            String farmName,
            Integer stockQuantity,
            Boolean featured,
            Boolean organic
    ) {
    }

    public record ProductDetail(
            Long id,
            String name,
            String category,
            java.math.BigDecimal price,
            Double rating,
            String imageUrl,
            String publisherName,
            String description,
            String origin,
            String farmName,
            String traceabilityCode,
            String certificate,
            LocalDate plantingDate,
            LocalDate harvestDate,
            String unit,
            Integer stockQuantity,
            Integer lowStockThreshold,
            Boolean organic,
            Boolean featured
    ) {
    }

    public record ReviewRequest(Long orderItemId, @Min(1) @Max(5) int rating, @NotBlank String content) {
    }

    public record ReviewResponse(Long id, String userName, Integer rating, String content, LocalDateTime createdAt) {
    }

    public record ReviewEligibilityResponse(boolean canReview, boolean reviewed, String message, Long orderItemId) {
    }
}
