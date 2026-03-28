package com.zhouri.farmshop.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class LegacyReviewIndexMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void dropsLegacyUniqueIndexWhenPresent() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("uk_reviews_user_product"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("reviews"), eq("idx_reviews_product_id"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("reviews"), eq("idx_reviews_user_id"))).thenReturn(0);

        var migration = new LegacyReviewIndexMigration(jdbcTemplate);
        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).execute("CREATE INDEX idx_reviews_product_id ON reviews (product_id)");
        verify(jdbcTemplate).execute("CREATE INDEX idx_reviews_user_id ON reviews (user_id)");
        verify(jdbcTemplate).execute("ALTER TABLE reviews DROP INDEX uk_reviews_user_product");
    }

    @Test
    void skipsDropWhenLegacyUniqueIndexAbsent() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("uk_reviews_user_product"))).thenReturn(0);

        var migration = new LegacyReviewIndexMigration(jdbcTemplate);
        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void skipsCreatingFallbackIndexesWhenAlreadyPresent() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("uk_reviews_user_product"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("reviews"), eq("idx_reviews_product_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("reviews"), eq("idx_reviews_user_id"))).thenReturn(1);

        var migration = new LegacyReviewIndexMigration(jdbcTemplate);
        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, times(1)).execute("ALTER TABLE reviews DROP INDEX uk_reviews_user_product");
        verify(jdbcTemplate, never()).execute("CREATE INDEX idx_reviews_product_id ON reviews (product_id)");
        verify(jdbcTemplate, never()).execute("CREATE INDEX idx_reviews_user_id ON reviews (user_id)");
    }
}
