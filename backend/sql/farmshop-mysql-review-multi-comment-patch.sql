USE farmshop;

SET NAMES utf8mb4;

SET @drop_review_unique_index = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'reviews'
        AND index_name = 'uk_reviews_user_product'
    ),
    'ALTER TABLE reviews DROP INDEX uk_reviews_user_product',
    'SELECT ''uk_reviews_user_product already absent'''
  )
);
PREPARE stmt FROM @drop_review_unique_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customer_orders
SET status = 'RECEIVED'
WHERE id = 1 AND status = 'DELIVERED';

INSERT INTO reviews (product_id, user_id, rating, content, created_at)
SELECT 1, 1, 4, '第二次购买还是很新鲜，适合继续回购。', NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM reviews
  WHERE product_id = 1
    AND user_id = 1
    AND content = '第二次购买还是很新鲜，适合继续回购。'
);
