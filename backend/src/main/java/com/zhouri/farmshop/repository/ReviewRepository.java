package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.Review;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewRepository {

    @Select("""
            SELECT id, product_id, user_id, rating, content, created_at
            FROM reviews
            WHERE product_id = #{productId}
            ORDER BY created_at DESC
            """)
    @Results(id = "reviewResultMap", value = {
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "createdAt", column = "created_at")
    })
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Select("SELECT COUNT(*) > 0 FROM reviews WHERE user_id = #{userId} AND product_id = #{productId}")
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Insert("""
            INSERT INTO reviews (product_id, user_id, rating, content, created_at)
            VALUES (#{product.id}, #{user.id}, #{rating}, #{content}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Review review);

    default Review save(Review review) {
        if (review.getCreatedAt() == null) {
            review.setCreatedAt(LocalDateTime.now());
        }
        insert(review);
        return review;
    }

    default List<Review> saveAll(List<Review> reviews) {
        for (Review review : reviews) {
            save(review);
        }
        return reviews;
    }
}
