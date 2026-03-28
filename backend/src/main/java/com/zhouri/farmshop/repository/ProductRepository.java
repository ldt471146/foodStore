package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.Product;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductRepository {

    @Select("""
            SELECT id, created_by_user_id, name, category, price, stock_quantity, unit, description, image_url, farm_name, origin,
                   certificate, traceability_code, planting_date, harvest_date, organic, featured, rating,
                   low_stock_threshold, created_at, updated_at
            FROM products
            ORDER BY id
            """)
    @Results(id = "productResultMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "createdBy", column = "created_by_user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "stockQuantity", column = "stock_quantity"),
            @Result(property = "imageUrl", column = "image_url"),
            @Result(property = "farmName", column = "farm_name"),
            @Result(property = "traceabilityCode", column = "traceability_code"),
            @Result(property = "plantingDate", column = "planting_date"),
            @Result(property = "harvestDate", column = "harvest_date"),
            @Result(property = "lowStockThreshold", column = "low_stock_threshold"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Product> findAll();

    @Select("""
            SELECT id, created_by_user_id, name, category, price, stock_quantity, unit, description, image_url, farm_name, origin,
                   certificate, traceability_code, planting_date, harvest_date, organic, featured, rating,
                   low_stock_threshold, created_at, updated_at
            FROM products
            WHERE id = #{id}
            """)
    @ResultMap("productResultMap")
    Product findByIdOrNull(Long id);

    default Optional<Product> findById(Long id) {
        return Optional.ofNullable(findByIdOrNull(id));
    }

    @Select("""
            SELECT id, created_by_user_id, name, category, price, stock_quantity, unit, description, image_url, farm_name, origin,
                   certificate, traceability_code, planting_date, harvest_date, organic, featured, rating,
                   low_stock_threshold, created_at, updated_at
            FROM products
            WHERE featured = TRUE
            ORDER BY rating DESC
            """)
    @ResultMap("productResultMap")
    List<Product> findByFeaturedTrueOrderByRatingDesc();

    @Select("""
            SELECT id, created_by_user_id, name, category, price, stock_quantity, unit, description, image_url, farm_name, origin,
                   certificate, traceability_code, planting_date, harvest_date, organic, featured, rating,
                   low_stock_threshold, created_at, updated_at
            FROM products
            WHERE LOWER(category) = LOWER(#{category})
            ORDER BY id
            """)
    @ResultMap("productResultMap")
    List<Product> findByCategoryIgnoreCase(String category);

    @Select("SELECT COUNT(*) FROM products")
    long count();

    @Insert("""
            INSERT INTO products (created_by_user_id, name, category, price, stock_quantity, unit, description, image_url, farm_name, origin,
                                  certificate, traceability_code, planting_date, harvest_date, organic, featured, rating,
                                  low_stock_threshold, created_at, updated_at)
            VALUES (#{createdBy.id}, #{name}, #{category}, #{price}, #{stockQuantity}, #{unit}, #{description}, #{imageUrl}, #{farmName}, #{origin},
                    #{certificate}, #{traceabilityCode}, #{plantingDate}, #{harvestDate}, #{organic}, #{featured}, #{rating},
                    #{lowStockThreshold}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("""
            UPDATE products
            SET created_by_user_id = #{createdBy.id},
                name = #{name},
                category = #{category},
                price = #{price},
                stock_quantity = #{stockQuantity},
                unit = #{unit},
                description = #{description},
                image_url = #{imageUrl},
                farm_name = #{farmName},
                origin = #{origin},
                certificate = #{certificate},
                traceability_code = #{traceabilityCode},
                planting_date = #{plantingDate},
                harvest_date = #{harvestDate},
                organic = #{organic},
                featured = #{featured},
                rating = #{rating},
                low_stock_threshold = #{lowStockThreshold},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(Product product);

    default Product save(Product product) {
        var now = LocalDateTime.now();
        if (product.getId() == null) {
            if (product.getCreatedAt() == null) {
                product.setCreatedAt(now);
            }
            if (product.getUpdatedAt() == null) {
                product.setUpdatedAt(now);
            }
            insert(product);
        } else {
            product.setUpdatedAt(now);
            update(product);
        }
        return product;
    }

    default List<Product> saveAll(List<Product> products) {
        for (Product product : products) {
            save(product);
        }
        return products;
    }

    @Delete("DELETE FROM products WHERE id = #{id}")
    void deleteById(Long id);
}
