CREATE DATABASE IF NOT EXISTS farmshop
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE farmshop;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS browse_records;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS inventory_movements;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS customer_orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS app_users;

CREATE TABLE app_users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(80) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  phone VARCHAR(30),
  address VARCHAR(255),
  avatar_color VARCHAR(20),
  avatar_image_url VARCHAR(255),
  role VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_by_user_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(60) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  stock_quantity INT NOT NULL,
  unit VARCHAR(20) NOT NULL,
  description TEXT NOT NULL,
  image_url VARCHAR(255),
  farm_name VARCHAR(120),
  origin VARCHAR(120),
  certificate VARCHAR(120),
  traceability_code VARCHAR(80) UNIQUE,
  planting_date DATE,
  harvest_date DATE,
  organic BOOLEAN NOT NULL,
  featured BOOLEAN NOT NULL,
  rating DOUBLE NOT NULL,
  low_stock_threshold INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_products_creator FOREIGN KEY (created_by_user_id) REFERENCES app_users(id)
);

CREATE TABLE customer_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  recipient_name VARCHAR(80) NOT NULL,
  recipient_phone VARCHAR(30) NOT NULL,
  recipient_address VARCHAR(255) NOT NULL,
  note VARCHAR(255),
  logistics_company VARCHAR(80),
  tracking_number VARCHAR(80),
  total_amount DECIMAL(10, 2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  payment_status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  paid_at TIMESTAMP NULL,
  shipped_at TIMESTAMP NULL,
  delivered_at TIMESTAMP NULL,
  CONSTRAINT fk_customer_orders_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);

CREATE TABLE order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  unit_price DECIMAL(10, 2) NOT NULL,
  quantity INT NOT NULL,
  subtotal DECIMAL(10, 2) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES customer_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE inventory_movements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  source VARCHAR(120),
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_inventory_movements_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES app_users(id),
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uk_cart_items_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE browse_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  viewed_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_browse_records_user FOREIGN KEY (user_id) REFERENCES app_users(id),
  CONSTRAINT fk_browse_records_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);

SET FOREIGN_KEY_CHECKS = 1;
