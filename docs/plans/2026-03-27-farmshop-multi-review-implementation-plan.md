# 商品多次评论 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 允许已确认收货的消费者对同一商品多次发表评论，并同步调整前后端提示与数据库约束。

**Architecture:** 保持现有评论表结构不变，只删除 `(user_id, product_id)` 唯一约束。后端评论资格从“是否评论过”改为“是否已确认收货”，前端只展示是否可评论与中文提示，不再阻断追加评论。

**Tech Stack:** Spring Boot、MyBatis、MySQL、Vue 3、TypeScript

---

### Task 1: 锁定评论多次追加的失败测试

**Files:**
- Create: `backend/src/test/java/com/zhouri/farmshop/service/CatalogServiceMultiReviewTest.java`

### Task 2: 修改后端评论资格与新增评论逻辑

**Files:**
- Modify: `backend/src/main/java/com/zhouri/farmshop/service/CatalogService.java`
- Modify: `backend/src/main/java/com/zhouri/farmshop/repository/ReviewRepository.java`

### Task 3: 修改 MySQL 建表与补丁脚本

**Files:**
- Modify: `backend/sql/farmshop-mysql-init.sql`
- Modify: `backend/sql/farmshop-mysql-seed.sql`
- Create: `backend/sql/farmshop-mysql-review-multi-comment-patch.sql`

### Task 4: 修改前端订单页和商品详情页提示

**Files:**
- Modify: `frontend/src/pages/OrdersPage.vue`
- Modify: `frontend/src/pages/ProductDetailPage.vue`

### Task 5: 运行验证

**Files:**
- Test: `backend/src/test/java/com/zhouri/farmshop/service/CatalogServiceMultiReviewTest.java`
- Build: `frontend`
