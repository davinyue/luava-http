# 移除 Apache URI 查询构建工具使用

## 背景

`HttpRequestUtils` 当前使用 Apache HttpClient 的 `URIBuilder` 和 `URLEncodedUtils` 组合请求地址与查询参数，需要改用其他实现方案。

## 目标

移除 `HttpRequestUtils` 对 `URIBuilder` 和 `URLEncodedUtils` 的导入和调用，同时保持查询参数、已有查询串及 URI 片段的组合行为。

## 范围

- 调整 `HttpRequestUtils` 的请求 URI 构建逻辑。
- 执行与 HTTP 请求工具直接相关的测试。

## 非目标

- 不移除项目对 Apache HttpClient 的整体依赖。
- 不调整 `Qs` 的序列化规则或其他 HTTP 请求 API。

## 关键设计

使用 JDK `URI` 校验和解析原始地址，将 `Qs` 生成的已编码查询参数插入 URI 片段标识之前，并根据原 URI 是否已有查询串选择分隔符。字符串查询参数遵循 `Qs.stringify` 的既有契约保持原样。

## 验收标准

- `HttpRequestUtils` 不再导入或调用 `URIBuilder`、`URLEncodedUtils`。
- 原 URL 的已有查询参数、新增 UTF-8 查询参数和片段可正确组合。
- 相关测试通过。

## 任务清单

### 分析

- [x] 明确需求目标、范围和非目标
- [x] 确认影响文件、接口、配置或数据结构

### 实施

- [x] 完成核心修改或实现
- [x] 保持现有风格与最小必要改动

### 验证

- [x] 执行与改动直接相关的测试、构建或检查
- [x] 记录无法验证的原因与风险

### 收尾

- [x] 更新当前进度
- [x] 记录剩余风险与阻塞

## 当前进度

实现与验证均已完成。执行 `mvn clean -Dtest=HttpRequestUtilsTest,QsTest test`，共 14 个测试通过。

## 风险与阻塞

暂无风险与阻塞。
