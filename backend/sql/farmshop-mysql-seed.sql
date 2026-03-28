USE farmshop;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM reviews;
DELETE FROM browse_records;
DELETE FROM cart_items;
DELETE FROM inventory_movements;
DELETE FROM order_items;
DELETE FROM customer_orders;
DELETE FROM products;
DELETE FROM app_users;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO app_users (
  id, username, password_hash, full_name, email, phone, address, avatar_color, avatar_image_url, role, created_at
) VALUES
  (1, 'consumer', '$2b$12$evcIfIxHMjoC7HlwU2Ygf.PKlBlI8vCR3OaV7HK4YeMBSnZgqQGnu', '普通消费者', 'consumer@farmshop.local', '13800000001', '杭州市余杭区农园路 1 号', '#8b9d77', NULL, 'CONSUMER', NOW()),
  (2, 'farmadmin', '$2b$12$evcIfIxHMjoC7HlwU2Ygf.PKlBlI8vCR3OaV7HK4YeMBSnZgqQGnu', '周农场主', 'farmadmin@farmshop.local', '13800000002', '湖州市安吉县周记农场', '#b1773a', NULL, 'FARM_ADMIN', NOW()),
  (3, 'platform', '$2b$12$evcIfIxHMjoC7HlwU2Ygf.PKlBlI8vCR3OaV7HK4YeMBSnZgqQGnu', '平台管理员', 'platform@farmshop.local', '13800000003', '平台运营中心', '#75a191', NULL, 'PLATFORM_ADMIN', NOW());

INSERT INTO products (
  id, created_by_user_id, name, category, price, stock_quantity, unit, description, image_url, farm_name, origin,
  certificate, traceability_code, planting_date, harvest_date, organic, featured, rating, low_stock_threshold, created_at, updated_at
) VALUES
  (1, 2, '晨露羽衣甘蓝', '蔬菜', 38.00, 120, '把', '清晨现采的有机羽衣甘蓝，适合轻食和果昔。', '/api/admin/product-images/sample-kale.png', '周记农场', '安吉',
   '绿色农场认证', 'TRC-VEG-001', DATE_SUB(CURDATE(), INTERVAL 90 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), 1, 1, 4.8, 20, NOW(), NOW()),
  (2, 2, '阳光番茄礼盒', '蔬菜', 56.00, 80, '盒', '酸甜平衡的小番茄礼盒，适合家庭沙拉。', '/api/admin/product-images/sample-tomato.png', '周记农场', '临安',
   '绿色农场认证', 'TRC-VEG-002', DATE_SUB(CURDATE(), INTERVAL 80 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1, 1, 4.6, 20, NOW(), NOW()),
  (3, 2, '龙井春茶', '茶饮', 128.00, 36, '罐', '手采春茶，清香回甘。', '/api/admin/product-images/sample-tea.png', '周记农场', '西湖',
   '绿色农场认证', 'TRC-TEA-001', DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), 1, 0, 4.9, 10, NOW(), NOW());

INSERT INTO inventory_movements (
  id, product_id, type, quantity, source, remark, created_at
) VALUES
  (1, 1, 'INBOUND', 150, '系统初始化', '初始库存', NOW()),
  (2, 2, 'INBOUND', 100, '系统初始化', '初始库存', NOW()),
  (3, 3, 'INBOUND', 50, '系统初始化', '初始库存', NOW());

INSERT INTO customer_orders (
  id, code, user_id, recipient_name, recipient_phone, recipient_address, note, logistics_company, tracking_number,
  total_amount, status, payment_status, created_at, updated_at, paid_at, shipped_at, delivered_at
) VALUES
  (1, 'FS202603270001', 1, '普通消费者', '13800000001', '杭州市余杭区农园路 1 号', '测试已完成订单', '顺丰速运', 'SF202603270001',
   76.00, 'RECEIVED', 'PAID', NOW(), NOW(), NOW(), NOW(), NOW()),
  (2, 'FS202603270002', 1, '普通消费者', '13800000001', '杭州市余杭区农园路 1 号', '测试待支付订单', NULL, NULL,
   128.00, 'PENDING_PAYMENT', 'UNPAID', NOW(), NOW(), NULL, NULL, NULL);

INSERT INTO order_items (
  id, order_id, product_id, product_name, unit_price, quantity, subtotal
) VALUES
  (1, 1, 1, '晨露羽衣甘蓝', 38.00, 2, 76.00),
  (2, 2, 3, '龙井春茶', 128.00, 1, 128.00);

INSERT INTO inventory_movements (
  id, product_id, type, quantity, source, remark, created_at
) VALUES
  (4, 1, 'OUTBOUND', 2, '订单 FS202603270001', '用户支付后扣减库存', NOW());

INSERT INTO reviews (
  id, product_id, user_id, rating, content, created_at
) VALUES
  (1, 1, 1, 5, '新鲜度很好，做沙拉很合适。', NOW()),
  (2, 1, 1, 4, '第二次购买还是很新鲜，适合继续回购。', NOW());

ALTER TABLE app_users AUTO_INCREMENT = 10;
ALTER TABLE products AUTO_INCREMENT = 10;
ALTER TABLE inventory_movements AUTO_INCREMENT = 10;
ALTER TABLE customer_orders AUTO_INCREMENT = 10;
ALTER TABLE order_items AUTO_INCREMENT = 10;
ALTER TABLE reviews AUTO_INCREMENT = 10;

-- 登录账号
-- 用户名: consumer   密码: 123456
-- 用户名: farmadmin  密码: 123456
-- 用户名: platform   密码: 123456
