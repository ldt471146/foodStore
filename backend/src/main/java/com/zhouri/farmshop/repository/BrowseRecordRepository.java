package com.zhouri.farmshop.repository;

import com.zhouri.farmshop.domain.BrowseRecord;
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
public interface BrowseRecordRepository {

    @Select("""
            SELECT id, user_id, product_id, viewed_at
            FROM browse_records
            WHERE user_id = #{userId}
            ORDER BY viewed_at DESC
            LIMIT 20
            """)
    @Results(id = "browseRecordResultMap", value = {
            @Result(property = "user", column = "user_id",
                    one = @One(select = "com.zhouri.farmshop.repository.UserRepository.findByIdOrNull")),
            @Result(property = "product", column = "product_id",
                    one = @One(select = "com.zhouri.farmshop.repository.ProductRepository.findByIdOrNull")),
            @Result(property = "viewedAt", column = "viewed_at")
    })
    List<BrowseRecord> findTop20ByUserIdOrderByViewedAtDesc(Long userId);

    @Insert("""
            INSERT INTO browse_records (user_id, product_id, viewed_at)
            VALUES (#{user.id}, #{product.id}, #{viewedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BrowseRecord browseRecord);

    default BrowseRecord save(BrowseRecord browseRecord) {
        if (browseRecord.getViewedAt() == null) {
            browseRecord.setViewedAt(LocalDateTime.now());
        }
        insert(browseRecord);
        return browseRecord;
    }

    default List<BrowseRecord> saveAll(List<BrowseRecord> records) {
        for (BrowseRecord record : records) {
            save(record);
        }
        return records;
    }
}
