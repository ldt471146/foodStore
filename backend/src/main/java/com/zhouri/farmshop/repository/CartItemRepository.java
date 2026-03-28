package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.CartItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartItemRepository {

    @Select("""
            SELECT id, user_id, product_id, quantity, created_at, updated_at
            FROM cart_items
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            """)
    @Results(id = "cartItemResultMap", value = {
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("""
            SELECT id, user_id, product_id, quantity, created_at, updated_at
            FROM cart_items
            WHERE user_id = #{userId} AND product_id = #{productId}
            """)
    @Results(value = {
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    CartItem findByUserIdAndProductIdOrNull(Long userId, Long productId);

    default Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return Optional.ofNullable(findByUserIdAndProductIdOrNull(userId, productId));
    }

    @Select("""
            SELECT id, user_id, product_id, quantity, created_at, updated_at
            FROM cart_items
            WHERE id = #{id}
            """)
    @Results(value = {
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    CartItem findByIdOrNull(Long id);

    default Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(findByIdOrNull(id));
    }

    @Insert("""
            INSERT INTO cart_items (user_id, product_id, quantity, created_at, updated_at)
            VALUES (#{user.id}, #{product.id}, #{quantity}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CartItem cartItem);

    @Update("""
            UPDATE cart_items
            SET quantity = #{quantity},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(CartItem cartItem);

    default CartItem save(CartItem cartItem) {
        var now = LocalDateTime.now();
        if (cartItem.getId() == null) {
            if (cartItem.getCreatedAt() == null) {
                cartItem.setCreatedAt(now);
            }
            cartItem.setUpdatedAt(now);
            insert(cartItem);
        } else {
            cartItem.setUpdatedAt(now);
            update(cartItem);
        }
        return cartItem;
    }

    @Delete("DELETE FROM cart_items WHERE id = #{id}")
    void deleteById(Long id);

    default void delete(CartItem cartItem) {
        if (cartItem != null && cartItem.getId() != null) {
            deleteById(cartItem.getId());
        }
    }

    @Delete("DELETE FROM cart_items WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}
