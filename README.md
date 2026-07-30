# luava-http

`luava-http` 是一个基于 Apache HttpClient 4.x 的 Java 8 HTTP 请求工具库，提供同步请求、异步客户端、连接池管理、查询参数编码、JSON
转换和 multipart 文件上传能力。

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
    <version>0.1.4</version>
</dependency>
```

## 快速开始

`HttpRequestUtils` 使用 `HttpRequestSpec` 描述请求，并复用内部 HTTP 客户端和连接池。
带响应处理器的 `execute` 会自动关闭响应：

```java
import org.apache.http.client.methods.HttpGet;
import org.rdlinux.luava.http.HttpRequestSpec;
import org.rdlinux.luava.http.HttpRequestUtils;
import org.rdlinux.luava.http.HttpResponseHandlers;

try(HttpRequestUtils requestUtils = new HttpRequestUtils()){
String responseBody = requestUtils.execute(
        HttpRequestSpec.builder(
                        HttpGet.METHOD_NAME,
                        "http://127.0.0.1:8080/api/health")
                .build(),
        HttpResponseHandlers.asString());
    System.out.

println(responseBody);
}
```

需要访问状态码或响应头时使用返回原始响应的 `execute(HttpRequestSpec)`，并由调用方
关闭响应：

```java
try(CloseableHttpResponse response = requestUtils.execute(
        HttpRequestSpec.builder(
                        HttpGet.METHOD_NAME,
                        "http://127.0.0.1:8080/api/health")
                .build())){
int statusCode = response.getStatusLine().getStatusCode();
}
```

## 类型化同步请求

### 查询参数、请求头和请求级配置

查询参数可以使用对象、`Map` 或查询字符串。`HttpRequestOptions.Builder` 未设置的
字段继承当前连接池默认值：

```java
Map<String, Object> queryParams = new HashMap<>();
queryParams.

put("page",1);
queryParams.

put("pageSize",20);
queryParams.

put("keyword","luava");

HttpRequestOptions requestOptions = HttpRequestOptions.builder()
        .socketTimeout(30000)
        .redirectsEnabled(false)
        .build();

HttpRequestSpec request = HttpRequestSpec.builder(
                HttpGet.METHOD_NAME,
                "http://127.0.0.1:8080/api/users")
        .query(queryParams)
        .header("Authorization", "Bearer your-token")
        .options(requestOptions)
        .build();

String responseBody = requestUtils.execute(
        request, HttpResponseHandlers.asString());
```

`HttpRequestUtils` 只提供 `execute(HttpRequestSpec)` 请求入口。GET、POST、PUT、
PATCH、DELETE、HEAD、OPTIONS 或自定义方法均通过 Builder 的 `method` 参数表达。

### JSON 正文

`jsonBody` 会将普通对象转换为 JSON；传入 `String`、`StringBuilder` 或
`StringBuffer` 时保留其正文内容：

```java
Map<String, Object> requestBody = new HashMap<>();
requestBody.

put("name","张三");
requestBody.

put("age",20);

HttpRequestSpec request = HttpRequestSpec.builder(
                HttpPost.METHOD_NAME,
                "http://127.0.0.1:8080/api/users")
        .jsonBody(requestBody)
        .header("X-Request-Id", "request-001")
        .build();

UserInfo userInfo = requestUtils.execute(
        request, HttpResponseHandlers.asJson(UserInfo.class));
```

### 表单和文本正文

`formBody` 明确创建 UTF-8 的 `application/x-www-form-urlencoded` 实体；
`textBody` 使用调用方提供的 `ContentType`：

```java
Map<String, Object> form = new HashMap<>();
form.

put("username","admin");
form.

put("password","secret");

HttpRequestSpec formRequest = HttpRequestSpec.builder(
                HttpPost.METHOD_NAME,
                "http://127.0.0.1:8080/api/login")
        .formBody(form)
        .build();

HttpRequestSpec textRequest = HttpRequestSpec.builder(
                HttpPost.METHOD_NAME,
                "http://127.0.0.1:8080/api/text")
        .textBody("plain text", ContentType.TEXT_PLAIN)
        .build();
```

### multipart 和自定义 HttpEntity

multipart 请求先通过 Apache `MultipartEntityBuilder` 创建，再使用 `entityBody`
发送：

```java
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

HttpRequestSpec uploadRequest = HttpRequestSpec.builder(
                HttpPost.METHOD_NAME,
                "http://127.0.0.1:8080/api/files")
        .entityBody(multipartEntity)
        .build();

try(
CloseableHttpResponse response =
        requestUtils.execute(uploadRequest)){
        System.out.

println(response.getStatusLine().

getStatusCode());
        }
```

`jsonBody`、`formBody`、`textBody` 和 `entityBody` 互斥，同一个 Builder 重复设置
正文会立即抛出异常。

## 响应处理

`HttpResponseHandlers` 支持字符串、`Class`、`Type`、`JavaType`、
`TypeReference` 以及对应的 snake_case JSON 转换：

```java
List<UserInfo> users = requestUtils.execute(
        HttpRequestSpec.builder(
                        HttpGet.METHOD_NAME,
                        "http://127.0.0.1:8080/api/users")
                .build(),
        HttpResponseHandlers.asJson(
                new TypeReference<List<UserInfo>>() {
                }));
```

静态 `responseDataConversion`、`responseDataSnakeConversion` 和
`consumeResponseAsString` 可用于处理原始 `CloseableHttpResponse`。这些方法会
消费正文并关闭响应。

## 查询参数工具

`Qs.stringify` 可以单独将对象或 `Map` 转换为查询字符串：

```java
import org.rdlinux.luava.http.Qs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

Map<String, Object> params = new LinkedHashMap<>();
params.

put("name","张三");
params.

put("tags",Arrays.asList("java", "http"));

String queryString = Qs.stringify(params);
System.out.

println(queryString);
```

## 连接池配置

默认构造 `HttpRequestUtils` 时会创建并复用默认连接池：

```java
try(HttpRequestUtils requestUtils = new HttpRequestUtils()){
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

try(
HttpRequestUtils requestUtils = new HttpRequestUtils(connectPool)){
        // 使用自定义连接池发起多个请求
        }
```

配置项说明：

| 配置项                        |        默认值 | 说明                                 |
|----------------------------|-----------:|------------------------------------|
| `connectTimeout`           |    5000 ms | 建立 TCP 连接的超时时间                     |
| `socketTimeout`            |   10000 ms | 等待响应数据的超时时间                        |
| `connectionRequestTimeout` |   10000 ms | 从连接池获取连接的超时时间                      |
| `singleMaxActive`          |          5 | 每个目标路由的最大连接数                       |
| `allMaxActive`             |         40 | 所有路由的最大总连接数，不能小于 `singleMaxActive` |
| `keepAliveDuration`        |   60000 ms | 服务端未声明 Keep-Alive 时使用的持续时间         |
| `maxIdleTime`              | 1800000 ms | 连接允许保持空闲的最长时间                      |
| `cleanSleepTime`           |   30000 ms | 后台连接清理间隔                           |
| `retryCount`               |          3 | 同步客户端失败重试配置                        |
| `ioThreadCount`            |     CPU 数量 | 异步客户端 I/O 线程数                      |

以下配置约束会在构建客户端时检查：

- 超时时间不能小于 0。
- 连接数、生命周期时间和异步 I/O 线程数必须大于 0。
- `allMaxActive` 不能小于 `singleMaxActive`。
- `retryCount` 不能小于 0。

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

try(
CloseableHttpClient client = new CloseableHttpClientBuilder()
        .setConnectPool(connectPool)
        .build();
CloseableHttpResponse response = client.execute(
        new HttpGet("http://127.0.0.1:8080/api/health"))){
        // 处理响应
        }
```

关闭客户端时，连接池和后台空闲连接清理线程会一并停止。

## 与 RestTemplate 结合使用

当前项目使用 Apache HttpClient 4.5，适用于 Spring Framework 5.x 或 Spring Boot 2.x 的
`HttpComponentsClientHttpRequestFactory`。建议将请求工厂和 `RestTemplate` 都注册为单例 Bean：

```java
import org.apache.http.impl.client.CloseableHttpClient;
import org.rdlinux.luava.http.CloseableHttpClientBuilder;
import org.rdlinux.luava.http.ConnectPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate HTTP连接池配置。
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建由Spring管理生命周期的HTTP请求工厂。
     *
     * @return HTTP请求工厂
     */
    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory() {
        ConnectPool connectPool = new ConnectPool()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(3000)
                .setSingleMaxActive(20)
                .setAllMaxActive(100)
                .setKeepAliveDuration(60000)
                .setMaxIdleTime(300000)
                .setCleanSleepTime(30000)
                .setRetryCount(3);

        CloseableHttpClient httpClient = new CloseableHttpClientBuilder()
                .setConnectPool(connectPool)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * 创建复用HTTP连接池的RestTemplate。
     *
     * @param requestFactory HTTP请求工厂
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(
            HttpComponentsClientHttpRequestFactory requestFactory) {
        return new RestTemplate(requestFactory);
    }
}
```

`HttpComponentsClientHttpRequestFactory` 实现了 Spring 的销毁回调。应用停止时，Spring 会关闭其持有的 `CloseableHttpClient`
，随后连接池和后台空闲连接清理线程会一并停止。

使用时应注意：

- `HttpComponentsClientHttpRequestFactory` 必须作为 Spring Bean 管理，不要仅在 `new RestTemplate(...)` 中创建临时实例。
- `RestTemplate` 应作为单例复用，不要为每次请求创建新实例。
- 不要在应用运行期间手动关闭请求工厂持有的 `CloseableHttpClient`。
- Spring Framework 6.x 和 Spring Boot 3.x 的 `HttpComponentsClientHttpRequestFactory` 使用 Apache HttpClient
  5，不能直接接收本项目当前构建的 HttpClient 4 客户端。

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

try(
CloseableHttpAsyncClient client = new AsyncCloseableHttpClientBuilder()
        .setConnectPool(connectPool)
        .build()){
        client.

start();

Future<HttpResponse> future = client.execute(
        new HttpGet("http://127.0.0.1:8080/api/health"),
        null);

HttpResponse response = future.get(10, TimeUnit.SECONDS);
String responseBody = EntityUtils.toString(
        response.getEntity(), StandardCharsets.UTF_8);
    System.out.

println(responseBody);
}
```

异步客户端关闭时会停止 I/O Reactor、连接管理器和空闲连接清理线程。客户端关闭后不能再次启动。

## 资源管理

使用本项目时应遵守以下规则：

1. 一个业务组件尽量复用一个 `HttpRequestUtils`，不要为每次请求创建新实例。
2. 使用完成后关闭 `HttpRequestUtils`。
3. 每个原始 `CloseableHttpResponse` 都必须关闭，未关闭响应会持续占用池中连接。
4. `responseDataConversion` 系列方法会自动消费并关闭响应。
5. 不要在仍有请求执行时关闭客户端或 `HttpRequestUtils`。

在 Spring 等容器中，建议将 `HttpRequestUtils` 注册为单例 Bean，并在 Bean 销毁阶段调用 `close()`。

## 状态码和异常处理

- 4xx、5xx 响应不会自动转换为异常，调用方应检查 `response.getStatusLine().getStatusCode()`。
- `HttpRequestUtils` 会将请求阶段的 `IOException` 包装为
  `HttpRequestExecuteException`。该异常仍是 `RuntimeException`，并通过
  `getRequestWriteState()` 返回 `NOT_STARTED`、`STARTED` 或 `COMPLETED`。
- JSON 转换失败时也会抛出运行时异常。
- 建议在业务层记录请求目标、状态码和必要的错误响应内容。

请求正文写出状态只描述客户端侧进度，不能证明服务端是否已处理请求。带副作用的
请求在 `STARTED` 或 `COMPLETED` 后发生异常时，应结合幂等键或远程状态查询判断，
不要直接重放。

## 安全注意事项

`HttpRequestUtils` 以及同步、异步构建器默认使用系统信任库和标准 HTTPS 主机名
校验。生产环境应将服务端证书或签发它的 CA 正确加入运行环境信任库。

为兼容确实依赖自签名证书且尚未配置信任库的历史环境，可以显式启用不安全模式：

```java
try(HttpRequestUtils requestUtils =
        new HttpRequestUtils(new ConnectPool(), true)){
        // 仅限明确接受中间人攻击风险的兼容环境
        }

        try(
CloseableHttpClient client = new CloseableHttpClientBuilder()
        .setConnectPool(new ConnectPool())
        .setInsecureTrustAllCertificates(true)
        .build()){
        // 仅限兼容环境
        }
```

不安全模式会信任所有 TLS 证书并关闭主机名校验，不应在生产网络使用。

Trace 日志包含完整 URL、正文内容类型和长度，生产环境开启 Trace 日志前应确认
URL 查询参数中不包含令牌、密码或敏感业务数据。

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
