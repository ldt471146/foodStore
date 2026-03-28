package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserRepository {

    @Select("""
            SELECT id, username, password_hash, full_name, email, phone, address, avatar_color, avatar_image_url, role, created_at
            FROM app_users
            ORDER BY id
            """)
    @Results(id = "userResultMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "fullName", column = "full_name"),
            @Result(property = "avatarColor", column = "avatar_color"),
            @Result(property = "avatarImageUrl", column = "avatar_image_url"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<User> findAll();

    @Select("""
            SELECT id, username, password_hash, full_name, email, phone, address, avatar_color, avatar_image_url, role, created_at
            FROM app_users
            WHERE id = #{id}
            """)
    @ResultMap("userResultMap")
    User findByIdOrNull(Long id);

    default Optional<User> findById(Long id) {
        return Optional.ofNullable(findByIdOrNull(id));
    }

    @Select("""
            SELECT id, username, password_hash, full_name, email, phone, address, avatar_color, avatar_image_url, role, created_at
            FROM app_users
            WHERE LOWER(username) = LOWER(#{username})
            """)
    @ResultMap("userResultMap")
    User findByUsernameIgnoreCaseOrNull(String username);

    default Optional<User> findByUsernameIgnoreCase(String username) {
        return Optional.ofNullable(findByUsernameIgnoreCaseOrNull(username));
    }

    @Select("""
            SELECT id, username, password_hash, full_name, email, phone, address, avatar_color, avatar_image_url, role, created_at
            FROM app_users
            WHERE LOWER(email) = LOWER(#{email})
            """)
    @ResultMap("userResultMap")
    User findByEmailIgnoreCaseOrNull(String email);

    default Optional<User> findByEmailIgnoreCase(String email) {
        return Optional.ofNullable(findByEmailIgnoreCaseOrNull(email));
    }

    @Select("SELECT COUNT(*) FROM app_users")
    long count();

    @Select("SELECT COUNT(*) > 0 FROM app_users WHERE LOWER(username) = LOWER(#{username})")
    boolean existsByUsernameIgnoreCase(String username);

    @Select("SELECT COUNT(*) > 0 FROM app_users WHERE LOWER(email) = LOWER(#{email})")
    boolean existsByEmailIgnoreCase(String email);

    @Insert("""
            INSERT INTO app_users (username, password_hash, full_name, email, phone, address, avatar_color, avatar_image_url, role, created_at)
            VALUES (#{username}, #{passwordHash}, #{fullName}, #{email}, #{phone}, #{address}, #{avatarColor}, #{avatarImageUrl}, #{role}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
            UPDATE app_users
            SET username = #{username},
                password_hash = #{passwordHash},
                full_name = #{fullName},
                email = #{email},
                phone = #{phone},
                address = #{address},
                avatar_color = #{avatarColor},
                avatar_image_url = #{avatarImageUrl},
                role = #{role}
            WHERE id = #{id}
            """)
    int update(User user);

    default User save(User user) {
        if (user.getId() == null) {
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(LocalDateTime.now());
            }
            insert(user);
        } else {
            update(user);
        }
        return user;
    }
}
