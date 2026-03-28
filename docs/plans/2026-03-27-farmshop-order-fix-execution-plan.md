# 农场品商城订单链路修复执行计划

## 内部等级

- `M`

## 执行步骤

1. 审核订单相关失败项，锁定仓储与服务层
2. 修复 `OrderRepository` 的订单和订单项 `ResultMap`
3. 修复 `UserRepository`、`ProductRepository` 的关键主键显式映射
4. 做编译级校验，确认修改可通过
5. 等待用户自行重启后做运行时复测

## 回滚规则

- 仅修改仓储映射层，不触碰控制器接口签名
- 若编译失败，回到上一步继续修复，直到编译通过

## 验证命令

- `run-mvn-jdk17.cmd -q -DskipTests compile`

## 预期结果

- 订单对象 `id`、嵌套 `user.id`、`product.id` 可稳定映射
- 支付与后台订单写操作不再因为实体取回不完整而被错误拒绝
