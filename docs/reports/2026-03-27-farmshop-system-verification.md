# 农场品在线商城系统验收报告

## 验收范围

- 时间：2026-03-27
- 前端：`http://localhost:5173`
- 后端：`http://127.0.0.1:8080`
- 方式：真实 HTTP 请求 + Playwright 浏览器验收
- 目标：核对 [功能实现.md](/D:/aigent/zhouri/功能实现.md) 与已落地系统的一致性，标记通过、失败、阻塞项

## 总结

- 已通过：37 项
- 失败：0 项
- 阻塞/待复核：1 项

## 通过项

| 功能 | 结果 | 证据 |
| --- | --- | --- |
| 健康检查 | 通过 | `GET /api/health -> 200 {"status":"ok"}` |
| 三角色登录 | 通过 | `consumer`、`farmadmin`、`platform` 登录均 `200` |
| 当前用户信息 | 通过 | `GET /api/auth/me -> 200` |
| 用户注册 | 通过 | `POST /api/auth/register -> 200`，新建 `qa_20260327` |
| 找回密码 | 通过 | `POST /api/auth/forgot-password -> 200` |
| 重置后登录 | 通过 | `qa_20260327 / xyz123456 -> 200` |
| 首页推荐数据 | 通过 | `GET /api/catalog/home -> 200` |
| 商品列表 | 通过 | `GET /api/catalog/products -> 200` |
| 商品详情 | 通过 | `GET /api/catalog/products/1 -> 200` |
| 分类查询 | 通过 | `GET /api/catalog/categories -> 200` |
| 浏览记录写入 | 通过 | `POST /api/catalog/products/1/browse -> 200` |
| 推荐功能 | 通过 | `GET /api/catalog/recommendations -> 200`，返回个性化列表 |
| 评价列表查询 | 通过 | `GET /api/catalog/products/1/reviews -> 200` |
| 购物车查询 | 通过 | `GET /api/cart -> 200` |
| 加入购物车 | 通过 | `POST /api/cart/items -> 200` |
| 修改购物车数量 | 通过 | `PATCH /api/cart/items/2 -> 200` |
| 删除购物车项 | 通过 | `DELETE /api/cart/items/3 -> 200` |
| 提交订单 | 通过 | `POST /api/orders/checkout -> 200`，生成订单 `FS1774617386475554` |
| 管理台总览 | 通过 | `GET /api/admin/overview -> 200` |
| 商品管理查询 | 通过 | `GET /api/admin/products -> 200` |
| 商品新增 | 通过 | `POST /api/admin/products -> 200`，生成临时商品 `id=9` |
| 商品修改 | 通过 | `PUT /api/admin/products/9 -> 200` |
| 商品删除 | 通过 | `DELETE /api/admin/products/9 -> 200` |
| 库存流水查询 | 通过 | `GET /api/admin/inventory -> 200` |
| 库存入库登记 | 通过 | `POST /api/admin/inventory/movements -> 200`，新增流水 `id=21` |
| 客户画像 | 通过 | `GET /api/admin/customers -> 200` |
| 销售预测 | 通过 | `GET /api/admin/analytics/forecast -> 200` |
| 登录页隔离 | 通过 | 清空本地存储后访问 `/catalog`，页面显示独立登录页与中国果园背景 |
| 消费者权限跳转 | 通过 | 消费者访问 `/admin` 被前端路由拦回 `/home` |
| 农场管理员后台入口 | 通过 | `farmadmin` 登录后进入后台，页签含 `总览/商品/库存/库存报表/订单` |
| 平台管理员后台入口 | 通过 | `platform` 登录后进入后台，页签含 `总览/库存报表/订单/客户/预测` |
| 订单列表 | 通过 | `GET /api/orders -> 200`，订单 `id` 正常返回 |
| 订单详情 | 通过 | `GET /api/orders/6 -> 200`，订单 `id` 正常返回 |
| 模拟支付 | 通过 | `POST /api/orders/6/pay -> 200`，订单变为 `PAID/PROCESSING` |
| 后台订单处理 | 通过 | `PATCH /api/admin/orders/6/status -> 200` |
| 后台物流录入 | 通过 | `PATCH /api/admin/orders/6/logistics -> 200`，订单变为 `SHIPPED` |
| 支付后评价 | 通过 | `POST /api/catalog/products/6/reviews -> 200` |

## 失败项

| 功能 | 结果 | 现象 |
| --- | --- | --- |
| 无 | - | 订单链路回归后，原 5 项失败已消除 |

## 阻塞/待复核

| 功能 | 状态 | 说明 |
| --- | --- | --- |
| 前端登录后控制台无异常 | 待复核 | 浏览器登录流中曾出现 1 条 `403` 控制台错误，本轮未做浏览器网络面板级复核。 |

## 缺陷定位

### 1. 订单映射字段缺失

- 文件：[OrderRepository.java](/D:/aigent/zhouri/backend/src/main/java/com/zhouri/farmshop/repository/OrderRepository.java)
- 问题：多个 `@Results` 没有显式映射 `id`，导致 `findAll`、`findByIdOrNull`、`findByUserIdOrderByCreatedAtDesc` 读回的订单主键为 `null`
- 状态：已修复并回归通过

### 2. 支付与订单后台操作返回 403

- 文件：[OrderService.java](/D:/aigent/zhouri/backend/src/main/java/com/zhouri/farmshop/service/OrderService.java)
- 文件：[AdminService.java](/D:/aigent/zhouri/backend/src/main/java/com/zhouri/farmshop/service/AdminService.java)
- 文件：[OrderRepository.java](/D:/aigent/zhouri/backend/src/main/java/com/zhouri/farmshop/repository/OrderRepository.java)
- 现象：此前支付、状态更新、物流更新都被拒绝
- 状态：已修复并回归通过
- 结论：根因与订单、用户、商品的 MyBatis 主键映射不完整一致

## 结论

- 登录页隔离、三角色入口、商品管理、库存管理、客户画像、推荐、预测、订单支付、后台订单处理、物流录入、支付后评价均已跑通
- 当前剩余待复核项只剩浏览器控制台里的历史 `403` 资源错误是否完全消失
