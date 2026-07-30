# 移除 HTTP 请求旧 API

## 背景

`HttpRequestUtils` 已提供 `HttpRequestSpec` 类型化请求入口，原有
`httpRequest/getRequest/postRequest/.../WithOptions` 方法族继续保留会造成 API
重复、重载选择复杂和维护成本增加。`HttpRequestSpec` 当前仍通过 `formData`
布尔字段向执行器传递正文类型，无法直接表达 JSON、文本和任意 `HttpEntity`
之间的差异。

## 目标

- 将 `HttpRequestUtils` 收敛为以 `HttpRequestSpec` 为唯一请求描述的执行入口。
- 删除历史 HTTP 请求方法及其重载，不保留源码或二进制兼容。
- 让 `HttpRequestSpec` 直接保存类型明确的 `HttpEntity`，移除 `formData` 状态。
- 将测试和 README 全部迁移到新 API。

## 范围

- 删除 `HttpRequestUtils` 中 `httpRequest`、`httpRequestWithOptions` 及各 HTTP 动词
  请求方法。
- 保留 `execute(HttpRequestSpec)`、带响应处理器的 `execute`、静态响应转换方法、
  客户端生命周期和关闭能力。
- 修改 `HttpRequestSpec` 的正文构建和公开访问方法。
- 更新受影响的测试与 README 示例。

## 非目标

- 不移除静态响应转换方法。
- 不移除 `getHttpClient` 或修改连接池生命周期。
- 不调整异步客户端 API。
- 不升级依赖版本。

## 关键设计

- `HttpRequestSpec.Builder` 在 `jsonBody/formBody/textBody/entityBody` 调用时直接创建
  或保存 `HttpEntity`。
- `HttpRequestUtils` 只接收完整的 `HttpRequestSpec`，不再解释 `Object bodyParam`
  和 `boolean useFormData`。
- 正文互斥校验继续由 Builder 负责。
- 所有现有测试调用迁移到 `HttpRequestSpec.builder(...).build()`。

## 验收标准

- `HttpRequestUtils` 不再包含历史请求方法或 HTTP 动词便利重载。
- `HttpRequestSpec` 不再公开 `formData` 或通用 `bodyParam`。
- JSON、表单、文本和原始 `HttpEntity` 请求均可通过 Builder 表达。
- README 不再使用已删除的请求方法。
- 现有及迁移后的测试全部通过，项目构建和 Javadoc 检查通过。

## 任务清单

### 分析

- [x] 明确需求目标、范围和非目标
- [x] 确认影响文件、接口、配置或数据结构

### 实施

- [x] 重构 `HttpRequestSpec` 正文模型
- [x] 删除旧请求方法并收敛执行流程
- [x] 保持静态响应转换和生命周期 API 不变

### 验证

- [x] 迁移并执行相关测试
- [x] 检查 README 和源码不存在旧 API 调用
- [x] 执行构建和 Javadoc 检查

### 收尾

- [x] 更新当前进度
- [x] 记录剩余风险与阻塞

## 当前进度

需求已完成。`HttpRequestUtils` 已删除全部历史请求方法，只保留
`execute(HttpRequestSpec)` 请求入口；`HttpRequestSpec` 直接保存明确的
`HttpEntity`，不再包含 `formData` 或通用正文参数。仓库测试和 README 已全部
迁移，23 个测试、Maven package 和 Javadoc 检查均通过。

## 风险与阻塞

- 本次明确不保留旧 API 兼容，外部调用方必须迁移为
  `HttpRequestSpec.builder(...).build()` 后调用 `execute`。
- 无阻塞项。
