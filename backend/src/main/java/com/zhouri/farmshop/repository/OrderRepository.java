package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.Order;
import com.zhouri.farmshop.domain.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderRepository {

    @Select("""
            SELECT id, code, user_id, recipient_name, recipient_phone, recipient_address, note, logistics_company,
                   tracking_number, total_amount, status, payment_status, created_at, updated_at, paid_at, shipped_at, delivered_at
            FROM customer_orders
            ORDER BY created_at DESC
            """)
    @Results(id = "orderResultMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "note", column = "note"),
            @Result(property = "recipientName", column = "recipient_name"),
            @Result(property = "recipientPhone", column = "recipient_phone"),
            @Result(property = "recipientAddress", column = "recipient_address"),
            @Result(property = "logisticsCompany", column = "logistics_company"),
            @Result(property = "trackingNumber", column = "tracking_number"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "status", column = "status"),
            @Result(property = "paymentStatus", column = "payment_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "paidAt", column = "paid_at"),
            @Result(property = "shippedAt", column = "shipped_at"),
            @Result(property = "deliveredAt", column = "delivered_at"),
            @Result(property = "items", column = "id", many = @Many(select = "findItemsByOrderId"))
    })
    List<Order> findAll();

    @Select("""
            SELECT id, code, user_id, recipient_name, recipient_phone, recipient_address, note, logistics_company,
                   tracking_number, total_amount, status, payment_status, created_at, updated_at, paid_at, shipped_at, delivered_at
            FROM customer_orders
            WHERE id = #{id}
            """)
    @ResultMap("orderResultMap")
    Order findByIdOrNull(Long id);

    default Optional<Order> findById(Long id) {
        return Optional.ofNullable(findByIdOrNull(id));
    }

    @Select("""
            SELECT id, code, user_id, recipient_name, recipient_phone, recipient_address, note, logistics_company,
                   tracking_number, total_amount, status, payment_status, created_at, updated_at, paid_at, shipped_at, delivered_at
            FROM customer_orders
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            """)
    @ResultMap("orderResultMap")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("""
            SELECT id, order_id, product_id, product_name, unit_price, quantity, subtotal
            FROM order_items
            WHERE order_id = #{orderId}
            ORDER BY id
            """)
    @Results(id = "orderItemResultMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "order", column = "order_id",
                    one = @One(select = "findOrderSkeletonById")),
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "unitPrice", column = "unit_price"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "subtotal", column = "subtotal")
    })
    List<OrderItem> findItemsByOrderId(Long orderId);

    @Select("""
            SELECT id, code, user_id, recipient_name, recipient_phone, recipient_address, note, logistics_company,
                   tracking_number, total_amount, status, payment_status, created_at, updated_at, paid_at, shipped_at, delivered_at
            FROM customer_orders
            WHERE id = #{id}
            """)
    @Results(value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "note", column = "note"),
            @Result(property = "recipientName", column = "recipient_name"),
            @Result(property = "recipientPhone", column = "recipient_phone"),
            @Result(property = "recipientAddress", column = "recipient_address"),
            @Result(property = "logisticsCompany", column = "logistics_company"),
            @Result(property = "trackingNumber", column = "tracking_number"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "status", column = "status"),
            @Result(property = "paymentStatus", column = "payment_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "paidAt", column = "paid_at"),
            @Result(property = "shippedAt", column = "shipped_at"),
            @Result(property = "deliveredAt", column = "delivered_at")
    })
    Order findOrderSkeletonById(Long id);

    @Insert("""
            INSERT INTO customer_orders (code, user_id, recipient_name, recipient_phone, recipient_address, note,
                                         logistics_company, tracking_number, total_amount, status, payment_status,
                                         created_at, updated_at, paid_at, shipped_at, delivered_at)
            VALUES (#{code}, #{user.id}, #{recipientName}, #{recipientPhone}, #{recipientAddress}, #{note},
                    #{logisticsCompany}, #{trackingNumber}, #{totalAmount}, #{status}, #{paymentStatus},
                    #{createdAt}, #{updatedAt}, #{paidAt}, #{shippedAt}, #{deliveredAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    @Update("""
            UPDATE customer_orders
            SET code = #{code},
                user_id = #{user.id},
                recipient_name = #{recipientName},
                recipient_phone = #{recipientPhone},
                recipient_address = #{recipientAddress},
                note = #{note},
                logistics_company = #{logisticsCompany},
                tracking_number = #{trackingNumber},
                total_amount = #{totalAmount},
                status = #{status},
                payment_status = #{paymentStatus},
                updated_at = #{updatedAt},
                paid_at = #{paidAt},
                shipped_at = #{shippedAt},
                delivered_at = #{deliveredAt}
            WHERE id = #{id}
            """)
    int updateOrder(Order order);

    @Insert("""
            INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal)
            VALUES (#{order.id}, #{product.id}, #{productName}, #{unitPrice}, #{quantity}, #{subtotal})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrderItem(OrderItem item);

    @Delete("DELETE FROM order_items WHERE order_id = #{orderId}")
    int deleteItemsByOrderId(Long orderId);

    default Order save(Order order) {
        var now = LocalDateTime.now();
        if (order.getId() == null) {
            if (order.getCreatedAt() == null) {
                order.setCreatedAt(now);
            }
            if (order.getUpdatedAt() == null) {
                order.setUpdatedAt(now);
            }
            insertOrder(order);
        } else {
            order.setUpdatedAt(now);
            updateOrder(order);
            deleteItemsByOrderId(order.getId());
        }
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
                insertOrderItem(item);
            }
        }
        return order;
    }
}
