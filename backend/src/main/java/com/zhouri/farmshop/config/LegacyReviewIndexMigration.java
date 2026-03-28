package com.zhouri.farmshop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyReviewIndexMigration implements ApplicationRunner {

    private static final String REVIEW_UNIQUE_INDEX = "uk_reviews_user_product";
    private static final String REVIEW_PRODUCT_INDEX = "idx_reviews_product_id";
    private static final String REVIEW_USER_INDEX = "idx_reviews_user_id";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'reviews'
                      AND index_name = ?
                    """, Integer.class, REVIEW_UNIQUE_INDEX);
            if (count != null && count > 0) {
                ensureIndex("reviews", REVIEW_PRODUCT_INDEX, "product_id");
                ensureIndex("reviews", REVIEW_USER_INDEX, "user_id");
                jdbcTemplate.execute("ALTER TABLE reviews DROP INDEX " + REVIEW_UNIQUE_INDEX);
                log.info("Dropped legacy unique index {} on reviews table", REVIEW_UNIQUE_INDEX);
            }
        } catch (DataAccessException exception) {
            log.warn("Skipping legacy review index migration: {}", exception.getMessage());
        }
    }

    private void ensureIndex(String tableName, String indexName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + " (" + columnName + ")");
            log.info("Created fallback index {} on {}({})", indexName, tableName, columnName);
        }
    }
}
