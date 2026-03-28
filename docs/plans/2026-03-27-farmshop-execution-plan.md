# 农场品在线商城执行计划

## Internal Grade
`L`

## Wave Structure

### Wave 1：项目骨架
- 创建 `backend` Spring Boot 工程
- 创建 `frontend` Vue 3 + Vite + Tailwind 工程
- 落地统一 README 与 IDEA 运行说明

### Wave 2：后端核心域
- 建模用户、商品、订单、库存、评价、物流
- 接入 JWT 鉴权与角色控制
- 编写种子数据与基础 REST API

### Wave 3：前端主流程
- 完成自然有机风首页、商品页、详情页、购物车、订单页
- 完成登录注册、找回密码、评价与物流查询
- 完成管理后台控制台、商品、库存、订单、客户、分析页面

### Wave 4：验证与收尾
- 启动前后端
- 运行后端测试、前端构建
- 记录运行结果与 cleanup receipt

## Ownership Boundaries
- 后端负责领域规则、聚合计算、种子数据
- 前端负责视觉系统、状态管理、交互链路
- 所有验收都以 UI 可见结果和接口真实返回为准

## Verification Commands
- `set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot && mvn test`
- `set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot && mvn spring-boot:run`
- `npm install`
- `npm run build`

## Rollback Rules
- 每个波次结束前先运行对应验证命令
- 若后端模型调整导致接口破坏，优先回退最近一轮实体或 DTO 变更
- 若前端样式调整导致功能不可见，优先保留功能并回退视觉增强

## Cleanup Expectations
- 删除临时抽取目录与无效中间文件
- 记录最终运行命令、端口和必要环境变量
- 输出 `cleanup-receipt.json` 与 phase receipts
