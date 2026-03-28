package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.InventoryMovement;
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
public interface InventoryMovementRepository {

    @Select("""
            SELECT id, product_id, type, quantity, source, remark, created_at
            FROM inventory_movements
            ORDER BY created_at DESC
            """)
    @Results(id = "inventoryMovementResultMap", value = {
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "createdAt", column = "created_at")
    })
    List<InventoryMovement> findAll();

    @Select("""
            SELECT id, product_id, type, quantity, source, remark, created_at
            FROM inventory_movements
            WHERE product_id = #{productId}
            ORDER BY created_at DESC
            """)
    @Results(value = {
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "createdAt", column = "created_at")
    })
    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Insert("""
            INSERT INTO inventory_movements (product_id, type, quantity, source, remark, created_at)
            VALUES (#{product.id}, #{type}, #{quantity}, #{source}, #{remark}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InventoryMovement movement);

    default InventoryMovement save(InventoryMovement movement) {
        if (movement.getCreatedAt() == null) {
            movement.setCreatedAt(LocalDateTime.now());
        }
        insert(movement);
        return movement;
    }
}
