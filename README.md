# foodStore 农产品商城与运营管理平台

本项目是一个前后端分离的农产品商城与运营管理系统，包含：

- 消费者端：商品浏览、购物车、下单、支付、订单跟踪、评价
- 农场管理员端：商品管理、库存维护、订单发货、物流录入
- 平台管理员端：客户信息、经营分析、销量预测、预警总览

项目适合在 `IntelliJ IDEA` 中本地运行，也可以交给其他同学或老师按本文档完成部署。

## 仓库结构

```text
foodStore/
├─ backend/                 Spring Boot 3 + MyBatis + Spring Security
├─ frontend/                Vue 3 + Vite + TypeScript + Pinia
├─ docs/                    需求与执行文档
├─ natural-organic-style-pack/
└─ README.md
```

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Security、MyBatis、MySQL 8
- 前端：Node.js 20+、Vue 3、Vite、TypeScript、Tailwind CSS、Pinia、Axios
- 数据库：MySQL 8

## 环境要求

- `JDK 17`
- `Node.js 20+`
- `npm 10+`
- `MySQL 8.x`
- `IntelliJ IDEA`

## 一、数据库准备

### 方式 A：本机已有 MySQL

1. 创建数据库：

```sql
CREATE DATABASE farmshop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行表结构脚本：`backend/src/main/resources/schema.sql`
3. 执行种子数据脚本：`backend/sql/farmshop-mysql-seed.sql`

### 方式 B：使用 Docker 启动 MySQL

```bash
docker compose up -d mysql
```

首次启动后，再手动导入：

- `backend/src/main/resources/schema.sql`
- `backend/sql/farmshop-mysql-seed.sql`

默认连接信息：

- Host：`127.0.0.1`
- Port：`3306`
- Database：`farmshop`
- Username：`root`
- Password：`123456`

## 二、后端启动

后端支持环境变量覆盖配置：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

IDEA 中运行启动类：`backend/src/main/java/com/zhouri/farmshop/BackendApplication.java`

命令行运行：

```bash
cd backend
mvnw.cmd spring-boot:run
```

默认地址：`http://127.0.0.1:8080`

## 三、前端启动

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

## 四、IDEA 推荐运行顺序

1. 启动 MySQL，并导入数据库脚本
2. 在 IDEA 中运行 `BackendApplication`
3. 在 IDEA 终端进入 `frontend` 执行 `npm run dev`
4. 浏览器访问 `http://localhost:5173`

## 五、默认测试账号

- 消费者：`consumer / 123456`
- 农场管理员：`farmadmin / 123456`
- 平台管理员：`platform / 123456`

## 六、打包命令

### 前端构建

```bash
cd frontend
npm run build
```

### 后端测试

```bash
cd backend
mvnw.cmd test
```

### 后端打包

```bash
cd backend
mvnw.cmd clean package
```

## 七、常见问题

### 1）前端能开，后端接口报错

优先检查：

- MySQL 是否启动
- 数据库 `farmshop` 是否已创建
- `schema.sql` 与 `farmshop-mysql-seed.sql` 是否已执行
- 后端端口 `8080` 是否被占用

### 2）登录失败

优先检查：

- 是否导入了种子数据
- 用户表 `app_users` 中是否存在默认账号
- 密码是否为 `123456`
