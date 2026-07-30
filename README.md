# luava-http

`luava-http` 是一个基于 Apache HttpClient 4.x 的 Java 8 HTTP 请求工具库，提供同步请求、异步客户端、连接池管理、查询参数编码、JSON 转换和 multipart 文件上传能力。

## 功能特性

- 支持 GET、POST、PUT、PATCH、DELETE、HEAD、OPTIONS 请求。
- 支持 URL 查询参数、JSON 请求体和 `application/x-www-form-urlencoded` 表单。
- 支持通过 Apache `HttpEntity` 发送 multipart 文件。
- 支持连接池大小、超时、Keep-Alive、空闲清理和同步重试配置。
- 支持同步和异步 HTTP 客户端。
- 支持将 JSON 响应转换为 Java 对象、泛型对象和蛇形命名对象。
- `HttpRequestUtils`、连接池及后台清理线程具有统一的关闭生命周期。

## 环境要求

- JDK 8 或更高版本。
- Maven 3.x。

项目编译目标为 Java 8。

## Maven 依赖

```xml
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>luava-http</artifactId>
    <version>0.1.2</version>
</dependency>
```

## 快速开始

`HttpRequestUtils` 会复用内部连接池。工具对象和原始响应都需要关闭，建议始终使用 try-with-resources：

```java
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.nio.charset.StandardCharsets;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        try (HttpRequestUtils requestUtils = new HttpRequestUtils();
             CloseableHttpResponse response =
                     requestUtils.getRequest("http://127.0.0.1:8080/api/health")) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(
                    response.getEntity(), StandardCharsets.UTF_8);

            System.out.println("statusCode = " + statusCode);
            System.out.println("responseBody = " + responseBody);
        }
    }
}
```

## 同步请求教程

### GET 查询参数和请求头

对象或 `Map` 可以作为查询参数传入，参数会追加到 URL：

```java
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

Map<String, Object> queryParams = new HashMap<>();
queryParams.put("page", 1);
queryParams.put("pageSize", 20);
queryParams.put("keyword", "luava");

Header authorization = new BasicHeader("Authorization", "Bearer your-token");

try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response = requestUtils.getRequest(
             "http://127.0.0.1:8080/api/users",
             queryParams,
             authorization)) {
    String responseBody = EntityUtils.toString(
            response.getEntity(), StandardCharsets.UTF_8);
    System.out.println(responseBody);
}
```

### POST JSON

当请求体不是 `String` 或 `HttpEntity` 时，工具会自动将对象转换成 JSON，并使用 `application/json`：

```java
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

Map<String, Object> requestBody = new HashMap<>();
requestBody.put("name", "张三");
requestBody.put("age", 20);

try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response = requestUtils.postRequest(
             "http://127.0.0.1:8080/api/users",
             requestBody,
             new BasicHeader("X-Request-Id", "request-001"))) {
    String responseBody = EntityUtils.toString(
            response.getEntity(), StandardCharsets.UTF_8);
    System.out.println(responseBody);
}
```

`putRequest` 和 `patchRequest` 的调用方式与 `postRequest` 相同：

```java
try (CloseableHttpResponse response =
             requestUtils.putRequest("http://127.0.0.1:8080/api/users/1", requestBody)) {
    // 处理响应
}

try (CloseableHttpResponse response =
             requestUtils.patchRequest("http://127.0.0.1:8080/api/users/1", requestBody)) {
    // 处理响应
}
```

以上片段中的 `requestUtils` 应由外层 try-with-resources 或应用生命周期统一管理。

### 发送原始 JSON 字符串

传入 `String` 时不会再次进行 JSON 序列化：

```java
String json = "{\"name\":\"张三\",\"age\":20}";

try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response = requestUtils.postRequest(
             "http://127.0.0.1:8080/api/users", json)) {
    // 处理响应
}
```

### application/x-www-form-urlencoded 表单

`useFormData=true` 在当前 API 中表示使用 `application/x-www-form-urlencoded`，不是 multipart：

```java
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.util.HashMap;
import java.util.Map;

Map<String, Object> form = new HashMap<>();
form.put("username", "admin");
form.put("password", "secret");

try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response = requestUtils.httpRequest(
             HttpPost.METHOD_NAME,
             "http://127.0.0.1:8080/api/login",
             null,
             form,
             true)) {
    // 处理响应
}
```

### multipart 文件上传

multipart 请求通过 Apache `MultipartEntityBuilder` 创建，然后作为 `HttpEntity` 发送：

```java
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

File uploadFile = new File("/path/to/document.pdf");

HttpEntity multipartEntity = MultipartEntityBuilder.create()
        .addBinaryBody(
                "file",
                uploadFile,
                ContentType.APPLICATION_OCTET_STREAM,
                uploadFile.getName())
        .addTextBody(
                "description",
                "项目文档",
                ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
        .build();

try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response = requestUtils.postRequest(
             "http://127.0.0.1:8080/api/files",
             multipartEntity)) {
    int statusCode = response.getStatusLine().getStatusCode();
    System.out.println("statusCode = " + statusCode);
}
```

仓库中的 `FileUploadTest` 使用临时文件和内嵌 HTTP 服务提供了可重复运行的完整示例。

### DELETE、HEAD 和 OPTIONS

```java
try (CloseableHttpResponse response =
             requestUtils.deleteRequest("http://127.0.0.1:8080/api/users/1")) {
    // 处理删除响应
}

try (CloseableHttpResponse response =
             requestUtils.headRequest("http://127.0.0.1:8080/api/files/1")) {
    // 读取响应头
}

try (CloseableHttpResponse response =
             requestUtils.optionsRequest("http://127.0.0.1:8080/api/users")) {
    // 读取 Allow 等响应头
}
```

## JSON 响应转换

### 转换为普通 Java 对象

```java
try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response =
             requestUtils.getRequest("http://127.0.0.1:8080/api/users/1")) {
    UserInfo userInfo = HttpRequestUtils.responseDataConversion(
            response, UserInfo.class);
}
```

### 转换为泛型对象

```java
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

try (HttpRequestUtils requestUtils = new HttpRequestUtils();
     CloseableHttpResponse response =
             requestUtils.getRequest("http://127.0.0.1:8080/api/users")) {
    List<UserInfo> users = HttpRequestUtils.responseDataConversion(
            response,
            new TypeReference<List<UserInfo>>() {
            });
}
```

当 JSON 字段使用 `snake_case`，Java 属性使用小驼峰时，可以使用：

```java
UserInfo userInfo = HttpRequestUtils.responseDataSnakeConversion(
        response, UserInfo.class);
```

`responseDataConversion` 和 `responseDataSnakeConversion` 会消费响应体并关闭响应。调用后不要再次读取该响应。

## 查询参数工具

`Qs.stringify` 可以单独将对象或 `Map` 转换为查询字符串：

```java
import org.rdlinux.luava.http.Qs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

Map<String, Object> params = new LinkedHashMap<>();
params.put("name", "张三");
params.put("tags", Arrays.asList("java", "http"));

String queryString = Qs.stringify(params);
System.out.println(queryString);
```

## 连接池配置

默认构造 `HttpRequestUtils` 时会创建并复用默认连接池：

```java
try (HttpRequestUtils requestUtils = new HttpRequestUtils()) {
    // 在此生命周期内重复发起请求，内部复用同一个HTTP客户端和连接池
}
```

自定义配置示例：

```java
import org.rdlinux.luava.http.ConnectPool;
import org.rdlinux.luava.http.HttpRequestUtils;

ConnectPool connectPool = new ConnectPool()
        .setConnectTimeout(3000)
        .setSocketTimeout(10000)
        .setConnectionRequestTimeout(2000)
        .setSingleMaxActive(20)
        .setAllMaxActive(100)
        .setKeepAliveDuration(60000)
        .setMaxIdleTime(300000)
        .setCleanSleepTime(30000)
        .setRetryCount(3)
        .setIoThreadCount(4);

try (HttpRequestUtils requestUtils = new HttpRequestUtils(connectPool)) {
    // 使用自定义连接池发起多个请求
}
```

配置项说明：

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `connectTimeout` | 5000 ms | 建立 TCP 连接的超时时间 |
| `socketTimeout` | 10000 ms | 等待响应数据的超时时间 |
| `connectionRequestTimeout` | 10000 ms | 从连接池获取连接的超时时间 |
| `singleMaxActive` | 5 | 每个目标路由的最大连接数 |
| `allMaxActive` | 40 | 所有路由的最大总连接数，不能小于 `singleMaxActive` |
| `keepAliveDuration` | 60000 ms | 服务端未声明 Keep-Alive 时使用的持续时间 |
| `maxIdleTime` | 1800000 ms | 连接允许保持空闲的最长时间 |
| `cleanSleepTime` | 30000 ms | 后台连接清理间隔 |
| `retryCount` | 3 | 同步客户端失败重试配置 |
| `ioThreadCount` | CPU 数量 | 异步客户端 I/O 线程数 |
| `connectionManagerShared` | `false` | 当前内部所有权模式仅支持 `false` |

以下配置约束会在构建客户端时检查：

- 超时时间不能小于 0。
- 连接数、生命周期时间和异步 I/O 线程数必须大于 0。
- `allMaxActive` 不能小于 `singleMaxActive`。
- `retryCount` 不能小于 0。
- `connectionManagerShared=true` 不受支持，会抛出 `IllegalArgumentException`。

## 直接构建同步客户端

不使用 `HttpRequestUtils` 时，也可以直接构建 Apache `CloseableHttpClient`：

```java
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.rdlinux.luava.http.CloseableHttpClientBuilder;
import org.rdlinux.luava.http.ConnectPool;

ConnectPool connectPool = new ConnectPool()
        .setSingleMaxActive(10)
        .setAllMaxActive(50);

try (CloseableHttpClient client = new CloseableHttpClientBuilder()
        .setConnectPool(connectPool)
        .build();
     CloseableHttpResponse response = client.execute(
             new HttpGet("http://127.0.0.1:8080/api/health"))) {
    // 处理响应
}
```

关闭客户端时，连接池和后台空闲连接清理线程会一并停止。

## 异步客户端

异步客户端构建后必须调用 `start()`：

```java
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.http.AsyncCloseableHttpClientBuilder;
import org.rdlinux.luava.http.ConnectPool;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

ConnectPool connectPool = new ConnectPool()
        .setSingleMaxActive(20)
        .setAllMaxActive(100)
        .setIoThreadCount(4);

try (CloseableHttpAsyncClient client = new AsyncCloseableHttpClientBuilder()
        .setConnectPool(connectPool)
        .build()) {
    client.start();

    Future<HttpResponse> future = client.execute(
            new HttpGet("http://127.0.0.1:8080/api/health"),
            null);

    HttpResponse response = future.get(10, TimeUnit.SECONDS);
    String responseBody = EntityUtils.toString(
            response.getEntity(), StandardCharsets.UTF_8);
    System.out.println(responseBody);
}
```

异步客户端关闭时会停止 I/O Reactor、连接管理器和空闲连接清理线程。客户端关闭后不能再次启动。

## 资源管理

使用本项目时应遵守以下规则：

1. 一个业务组件尽量复用一个 `HttpRequestUtils`，不要为每次请求创建新实例。
2. 使用完成后关闭 `HttpRequestUtils`。
3. 每个原始 `CloseableHttpResponse` 都必须关闭，未关闭响应会持续占用池中连接。
4. `responseDataConversion` 系列方法会自动消费并关闭响应。
5. 不要设置 `connectionManagerShared=true`，当前连接管理器由客户端独占管理。
6. 不要在仍有请求执行时关闭客户端或 `HttpRequestUtils`。

在 Spring 等容器中，建议将 `HttpRequestUtils` 注册为单例 Bean，并在 Bean 销毁阶段调用 `close()`。

## 状态码和异常处理

- 4xx、5xx 响应不会自动转换为异常，调用方应检查 `response.getStatusLine().getStatusCode()`。
- `HttpRequestUtils` 会将请求阶段的 `IOException` 包装为 `RuntimeException`。
- JSON 转换失败时也会抛出运行时异常。
- 建议在业务层记录请求目标、状态码和必要的错误响应内容。

## 安全注意事项

`HttpRequestUtils` 以及显式配置了 `ConnectPool` 的同步、异步构建器会信任所有 TLS 证书，并关闭 HTTPS 主机名校验。因此：

- 仅应在可信网络或明确接受该风险的环境中使用当前 HTTPS 实现。
- 不要直接用于存在中间人攻击风险的生产网络。
- 生产使用前应改为系统默认信任库或显式配置受信任证书，并启用主机名校验。

Trace 日志可能包含完整 URL 和请求体，生产环境开启 Trace 日志前应确认其中不包含令牌、密码或敏感业务数据。

## 构建和测试

运行全部测试：

```bash
mvn clean test
```

构建 JAR、源码包和 Javadoc 包：

```bash
mvn clean package
```

测试均使用本地内嵌 HTTP 服务，不依赖外部接口。文件上传测试会动态创建并清理临时文件。

## 许可证

本项目使用 Apache License 2.0，详见 [LICENSE](LICENSE)。
