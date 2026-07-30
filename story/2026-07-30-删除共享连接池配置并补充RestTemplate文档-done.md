# 删除共享连接池配置并补充 RestTemplate 文档

## 背景

`connectionManagerShared` 原计划用于 RestTemplate 场景，但当前构建器始终在内部创建并持有连接管理器，无法向外部交付共享连接池的关闭责任。该配置只能被拒绝，不能产生有效行为，容易造成误解。

## 目标

删除无效的共享连接池配置，并明确 Spring 5、Spring Boot 2 环境下 RestTemplate 使用当前连接池的正确方式和生命周期。

## 范围

- 删除 `ConnectPool.connectionManagerShared` 及访问方法。
- 删除构建器中的对应校验和失效场景测试。
- 更新 README 连接池配置说明。
- 增加 RestTemplateConfig 完整示例和兼容性说明。

## 非目标

- 不支持外部注入或跨客户端共享连接管理器。
- 不升级 Apache HttpClient 5。
- 不引入 Spring 依赖。

## 关键设计

- 同步和异步构建器创建的连接管理器始终由客户端独占。
- 客户端关闭时统一关闭连接池和后台清理线程。
- RestTemplate 通过 Spring 管理的 `HttpComponentsClientHttpRequestFactory` 持有客户端，并在容器销毁阶段释放资源。

## 验收标准

- 生产代码中不再存在 `connectionManagerShared` 配置。
- 原有连接池生命周期测试继续通过。
- README 包含可复制的 RestTemplateConfig 示例。
- README 明确 Spring 5、Spring Boot 2 与当前 HttpClient 4 的兼容范围。

## 任务清单

### 分析

- [x] 明确需求目标、范围和非目标
- [x] 确认影响文件、接口、配置或数据结构

### 实施

- [x] 删除无效配置、校验和对应测试
- [x] 补充 RestTemplateConfig 使用文档

### 验证

- [x] 执行与改动直接相关的测试、构建或检查
- [x] 记录无法验证的原因与风险

### 收尾

- [x] 更新当前进度
- [x] 记录剩余风险与阻塞

## 当前进度

已删除 `connectionManagerShared` 配置、校验和失效场景测试，README 已增加 Spring 5、Spring Boot 2 环境下的 RestTemplateConfig 完整示例。`mvn clean test` 执行成功，共运行 7 个测试，无失败或错误。

## 风险与阻塞

- 删除公开方法属于源代码不兼容变更，仍调用 `setConnectionManagerShared` 的项目需要移除该调用。
- Spring Framework 6.x 和 Spring Boot 3.x 使用 Apache HttpClient 5，不在当前版本兼容范围内。
