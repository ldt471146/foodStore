# 农产品商城评论提交 403 修复执行计划

## 内部等级

- `L`

## 波次拆分

1. 冻结本轮需求与运行证据，补最小化接口测试
2. 重构评论资格为订单项级校验，修正后端评论提交流程
3. 同步前端评论提交参数与订单页状态判断
4. 运行后端测试，确认无回归

## 责任边界

- 后端：评论资格判定、评论提交接口契约、测试
- 前端：订单页与商品详情页评论提交参数、错误提示
- 文档：`vibe` 需求冻结、执行计划、阶段回执

## 验证命令

- `mvnw.cmd -q -Dtest=CatalogServiceMultiReviewTest,StorefrontControllerReviewSecurityTest test`
- `mvnw.cmd -q test`

## 回滚规则

- 不改登录鉴权主链路与 JWT 解析逻辑
- 若接口契约调整导致前端类型报错，优先补齐类型与调用参数，不回退到产品级资格模型

## 清理要求

- 输出 `phase-05-plan-execute.json` 与 `cleanup-receipt.json`
- 不保留临时调试脚本
