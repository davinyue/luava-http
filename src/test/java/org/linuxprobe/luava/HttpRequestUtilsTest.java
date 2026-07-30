package org.linuxprobe.luava;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.ConnectPool;
import org.rdlinux.luava.http.HttpRequestExecuteException;
import org.rdlinux.luava.http.HttpRequestOptions;
import org.rdlinux.luava.http.HttpRequestSpec;
import org.rdlinux.luava.http.HttpRequestUtils;
import org.rdlinux.luava.http.HttpResponseHandlers;
import org.rdlinux.luava.http.RequestWriteState;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HTTP请求工具测试。
 */
public class HttpRequestUtilsTest {

    /**
     * 验证请求响应和客户端均可正常关闭。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void requestTest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/request", exchange -> {
            byte[] responseBody = "success".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        try (HttpRequestUtils requestUtils = new HttpRequestUtils();
             CloseableHttpResponse response = requestUtils.execute(
                     HttpRequestSpec.builder(
                                     HttpPost.METHOD_NAME,
                                     "http://127.0.0.1:"
                                             + server.getAddress().getPort()
                                             + "/request")
                             .jsonBody("{}")
                             .build())) {
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertEquals("success", EntityUtils.toString(response.getEntity()));
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证单例请求工具可按请求控制是否跟随重定向。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldApplyRequestRedirectOption() throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            byte[] responseBody = "target".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        String redirectUrl = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/redirect";
        try (HttpRequestUtils requestUtils = new HttpRequestUtils()) {
            HttpRequestOptions noRedirect = new HttpRequestOptions(
                    1000, 1000, 1000, false);
            try (CloseableHttpResponse response =
                         requestUtils.execute(HttpRequestSpec.builder(
                                         HttpGet.METHOD_NAME, redirectUrl)
                                 .options(noRedirect)
                                 .build())) {
                Assert.assertEquals(
                        302, response.getStatusLine().getStatusCode());
            }

            HttpRequestOptions followRedirect = new HttpRequestOptions(
                    1000, 1000, 1000, true);
            try (CloseableHttpResponse response =
                         requestUtils.execute(HttpRequestSpec.builder(
                                         HttpGet.METHOD_NAME, redirectUrl)
                                 .options(followRedirect)
                                 .build())) {
                Assert.assertEquals(
                        200, response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                        "target", EntityUtils.toString(response.getEntity()));
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证单次请求读取超时覆盖共享客户端默认值。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldApplyPerRequestSocketTimeout() throws Exception {
        HttpServer server = createDelayedServer(300);
        server.start();
        String url = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/delayed";
        ConnectPool connectPool = new ConnectPool().setSocketTimeout(2000);
        try (HttpRequestUtils requestUtils =
                     new HttpRequestUtils(connectPool)) {
            HttpRequestOptions options = new HttpRequestOptions(
                    1000, 50, 1000, false);
            try {
                requestUtils.execute(HttpRequestSpec.builder(
                                HttpGet.METHOD_NAME, url)
                        .options(options)
                        .build());
                Assert.fail("请求级读取超时应覆盖客户端默认值");
            } catch (HttpRequestExecuteException expected) {
                Assert.assertEquals(
                        RequestWriteState.NOT_STARTED,
                        expected.getRequestWriteState());
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证请求工具保存构造时的连接池配置快照。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldSnapshotConnectPoolConfiguration() throws Exception {
        HttpServer server = createDelayedServer(300);
        server.start();
        String url = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/delayed";
        ConnectPool connectPool = new ConnectPool().setSocketTimeout(50);
        try (HttpRequestUtils requestUtils =
                     new HttpRequestUtils(connectPool)) {
            connectPool.setSocketTimeout(2000);
            try {
                requestUtils.execute(HttpRequestSpec.builder(
                                HttpGet.METHOD_NAME, url)
                        .build());
                Assert.fail("外部修改不应改变请求工具保存的连接池配置");
            } catch (HttpRequestExecuteException expected) {
                Assert.assertEquals(
                        RequestWriteState.NOT_STARTED,
                        expected.getRequestWriteState());
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证连接建立前失败时请求正文尚未写出。
     *
     * @throws Exception 创建临时端口失败
     */
    @Test
    public void shouldReportBodyNotStartedWhenConnectionFails()
            throws Exception {
        int unavailablePort;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            unavailablePort = serverSocket.getLocalPort();
        }
        ConnectPool connectPool = new ConnectPool()
                .setConnectTimeout(200)
                .setRetryCount(0);
        try (HttpRequestUtils requestUtils =
                     new HttpRequestUtils(connectPool)) {
            try {
                requestUtils.execute(HttpRequestSpec.builder(
                                HttpPost.METHOD_NAME,
                                "http://127.0.0.1:"
                                        + unavailablePort + "/request")
                        .jsonBody("{}")
                        .build());
                Assert.fail("不可用端口应连接失败");
            } catch (HttpRequestExecuteException expected) {
                Assert.assertEquals(
                        RequestWriteState.NOT_STARTED,
                        expected.getRequestWriteState());
            }
        }
    }

    /**
     * 验证正文完整写出后未获得响应时保留写出状态。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldReportCompletedBodyWhenServerDropsResponse()
            throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requestCount = new AtomicInteger();
        server.createContext("/drop", exchange -> {
            requestCount.incrementAndGet();
            consume(exchange.getRequestBody());
            exchange.close();
        });
        server.start();
        ConnectPool connectPool = new ConnectPool()
                .setSocketTimeout(500)
                .setRetryCount(0);
        try (HttpRequestUtils requestUtils =
                     new HttpRequestUtils(connectPool)) {
            try {
                requestUtils.execute(HttpRequestSpec.builder(
                                HttpPost.METHOD_NAME,
                                "http://127.0.0.1:"
                                        + server.getAddress().getPort()
                                        + "/drop")
                        .jsonBody("request-body")
                        .options(new HttpRequestOptions(
                                500, 500, 500, false))
                        .build());
                Assert.fail("服务端不返回响应时请求应失败");
            } catch (HttpRequestExecuteException expected) {
                Assert.assertEquals(
                        RequestWriteState.COMPLETED,
                        expected.getRequestWriteState());
            }
            Assert.assertEquals(1, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证单次请求配置拒绝非法超时。
     */
    @Test
    public void shouldRejectInvalidRequestOptions() {
        try {
            new HttpRequestOptions(-1, 1000, 1000, false);
            Assert.fail("负数超时应被拒绝");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals(
                    "timeout must be greater than or equal to 0",
                    expected.getMessage());
        }
    }

    /**
     * 验证类型化请求可为有正文和无正文请求添加请求头。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldApplyHeadersToRequests() throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> getHeader = new AtomicReference<>();
        AtomicReference<String> postHeader = new AtomicReference<>();
        AtomicReference<String> postBody = new AtomicReference<>();
        server.createContext("/headers", exchange -> {
            String headerValue =
                    exchange.getRequestHeaders().getFirst("X-Test");
            if ("GET".equals(exchange.getRequestMethod())) {
                getHeader.set(headerValue);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                postHeader.set(headerValue);
                postBody.set(readString(exchange.getRequestBody()));
            }
            byte[] responseBody = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/headers";
        try (HttpRequestUtils requestUtils = new HttpRequestUtils();
             CloseableHttpResponse getResponse = requestUtils.execute(
                     HttpRequestSpec.builder(HttpGet.METHOD_NAME, url)
                             .header("X-Test", "get")
                             .build());
             CloseableHttpResponse postResponse = requestUtils.execute(
                     HttpRequestSpec.builder(HttpPost.METHOD_NAME, url)
                             .header("X-Test", "post")
                             .build())) {
            EntityUtils.consume(getResponse.getEntity());
            EntityUtils.consume(postResponse.getEntity());
        } finally {
            server.stop(0);
        }
        Assert.assertEquals("get", getHeader.get());
        Assert.assertEquals("post", postHeader.get());
        Assert.assertEquals("", postBody.get());
    }

    /**
     * 验证 MDC 追踪头仅在调用方未显式设置时注入。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldInjectMdcHeaderWithoutDuplicatingExplicitHeader()
            throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<List<String>> automaticHeaders =
                new AtomicReference<>();
        AtomicReference<List<String>> explicitHeaders =
                new AtomicReference<>();
        AtomicInteger requestCount = new AtomicInteger();
        server.createContext("/trace", exchange -> {
            List<String> values = exchange.getRequestHeaders()
                    .get("x-b3-traceid");
            if (requestCount.getAndIncrement() == 0) {
                automaticHeaders.set(values);
            } else {
                explicitHeaders.set(values);
            }
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/trace";
        MDC.put("traceId", "mdc-trace");
        try (HttpRequestUtils requestUtils = new HttpRequestUtils();
             CloseableHttpResponse automatic =
                     requestUtils.execute(HttpRequestSpec.builder(
                                     HttpGet.METHOD_NAME, url)
                             .build());
             CloseableHttpResponse explicit = requestUtils.execute(
                     HttpRequestSpec.builder(HttpGet.METHOD_NAME, url)
                             .header("x-b3-traceid", "explicit-trace")
                             .build())) {
            EntityUtils.consume(automatic.getEntity());
            EntityUtils.consume(explicit.getEntity());
        } finally {
            MDC.clear();
            server.stop(0);
        }
        Assert.assertEquals(1, automaticHeaders.get().size());
        Assert.assertEquals("mdc-trace", automaticHeaders.get().get(0));
        Assert.assertEquals(1, explicitHeaders.get().size());
        Assert.assertEquals(
                "explicit-trace", explicitHeaders.get().get(0));
    }

    /**
     * 验证类型化请求可组合查询参数、正文、请求头和响应处理器。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldExecuteTypedRequestAndHandleJsonResponse()
            throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> rawQuery = new AtomicReference<>();
        AtomicReference<String> requestHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/typed", exchange -> {
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            requestHeader.set(
                    exchange.getRequestHeaders().getFirst("X-Test"));
            requestBody.set(readString(exchange.getRequestBody()));
            byte[] responseBody =
                    "{\"user_name\":\"typed-user\"}".getBytes(
                            StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.createContext("/list", exchange -> {
            byte[] responseBody =
                    "[\"first\",\"second\"]".getBytes(
                            StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://127.0.0.1:"
                + server.getAddress().getPort();
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("name", "张 三");
        queryParams.put("ids", new int[]{1, 2});
        Map<String, Object> jsonBody = new LinkedHashMap<>();
        jsonBody.put("message", "hello");
        HttpRequestSpec requestSpec = HttpRequestSpec.builder(
                        HttpPost.METHOD_NAME,
                        baseUrl + "/typed?existing=value#fragment")
                .query(queryParams)
                .jsonBody(jsonBody)
                .header("X-Test", "typed")
                .options(HttpRequestOptions.builder()
                        .redirectsEnabled(false)
                        .build())
                .build();
        try (HttpRequestUtils requestUtils = new HttpRequestUtils()) {
            UserPayload userPayload = requestUtils.execute(
                    requestSpec,
                    HttpResponseHandlers.asSnakeCaseJson(
                            UserPayload.class));
            Assert.assertEquals("typed-user", userPayload.getUserName());

            List<String> values = requestUtils.execute(
                    HttpRequestSpec.builder(
                                    HttpGet.METHOD_NAME,
                                    baseUrl + "/list")
                            .build(),
                    HttpResponseHandlers.asJson(
                            new TypeReference<List<String>>() {
                            }));
            Assert.assertEquals(2, values.size());
            Assert.assertEquals("second", values.get(1));
        } finally {
            server.stop(0);
        }

        List<NameValuePair> queryValues = URLEncodedUtils.parse(
                rawQuery.get(), StandardCharsets.UTF_8);
        Assert.assertTrue(containsParameter(
                queryValues, "existing", "value"));
        Assert.assertTrue(containsParameter(
                queryValues, "name", "张 三"));
        Assert.assertTrue(containsParameter(queryValues, "ids", "1"));
        Assert.assertTrue(containsParameter(queryValues, "ids", "2"));
        Assert.assertFalse(rawQuery.get().contains("fragment"));
        Assert.assertEquals("typed", requestHeader.get());
        Assert.assertTrue(requestBody.get().contains("\"message\":\"hello\""));
    }

    /**
     * 验证 Builder 请求配置只覆盖显式设置项。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void shouldApplyPartialRequestOptions() throws Exception {
        HttpServer server = createDelayedServer(300);
        server.start();
        String url = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/delayed";
        ConnectPool connectPool = new ConnectPool()
                .setConnectTimeout(1000)
                .setSocketTimeout(2000)
                .setConnectionRequestTimeout(1000);
        HttpRequestOptions options = HttpRequestOptions.builder()
                .socketTimeout(50)
                .build();
        Assert.assertFalse(options.hasConnectTimeout());
        Assert.assertTrue(options.hasSocketTimeout());
        try (HttpRequestUtils requestUtils =
                     new HttpRequestUtils(connectPool)) {
            try {
                requestUtils.execute(HttpRequestSpec.builder(
                                HttpGet.METHOD_NAME, url)
                        .options(options)
                        .build());
                Assert.fail("Builder 配置的读取超时应生效");
            } catch (HttpRequestExecuteException expected) {
                Assert.assertEquals(
                        RequestWriteState.NOT_STARTED,
                        expected.getRequestWriteState());
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证表单正文直接构建为带明确内容类型的 HTTP 实体。
     *
     * @throws Exception 正文读取失败
     */
    @Test
    public void shouldBuildExplicitFormEntity() throws Exception {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("userName", "张 三");
        HttpRequestSpec requestSpec = HttpRequestSpec.builder(
                        HttpPost.METHOD_NAME, "http://localhost/form")
                .formBody(form)
                .build();

        Assert.assertNotNull(requestSpec.getBodyEntity());
        Assert.assertTrue(
                requestSpec.getBodyEntity().getContentType().getValue()
                        .startsWith(
                                "application/x-www-form-urlencoded"));
        Assert.assertEquals(
                "userName=%E5%BC%A0+%E4%B8%89",
                EntityUtils.toString(requestSpec.getBodyEntity()));
    }

    /**
     * 验证类型化请求拒绝配置多个正文。
     */
    @Test
    public void shouldRejectMultipleTypedRequestBodies() {
        try {
            HttpRequestSpec.builder(HttpPost.METHOD_NAME, "http://localhost")
                    .jsonBody("{}")
                    .formBody("name=value");
            Assert.fail("同一请求不应允许配置多个正文");
        } catch (IllegalStateException expected) {
            Assert.assertEquals(
                    "request body already configured",
                    expected.getMessage());
        }
    }

    /**
     * 创建延迟响应服务。
     *
     * @param delayMillis 响应延迟毫秒数
     * @return 未启动的HTTP服务
     * @throws IOException 创建服务失败
     */
    private static HttpServer createDelayedServer(long delayMillis)
            throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/delayed", exchange -> {
            try {
                Thread.sleep(delayMillis);
                byte[] responseBody =
                        "delayed".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);
                exchange.getResponseBody().write(responseBody);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        return server;
    }

    /**
     * 消费输入流。
     *
     * @param inputStream 输入流
     * @throws IOException 读取失败
     */
    private static void consume(InputStream inputStream) throws IOException {
        try (InputStream input = inputStream) {
            byte[] buffer = new byte[1024];
            while (input.read(buffer) >= 0) {
                // 读取完整请求正文。
            }
        }
    }

    /**
     * 读取输入流字符串。
     *
     * @param inputStream 输入流
     * @return UTF-8 字符串
     * @throws IOException 读取失败
     */
    private static String readString(InputStream inputStream)
            throws IOException {
        try (InputStream input = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                output.write(buffer, 0, length);
            }
            return new String(
                    output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 判断查询参数集合是否包含指定键值。
     *
     * @param parameters 查询参数
     * @param name 参数名
     * @param value 参数值
     * @return 是否包含指定键值
     */
    private static boolean containsParameter(
            List<NameValuePair> parameters, String name, String value) {
        for (NameValuePair parameter : parameters) {
            if (name.equals(parameter.getName())
                    && value.equals(parameter.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * snake_case JSON 响应测试对象。
     */
    public static final class UserPayload {
        /**
         * 用户名称。
         */
        private String userName;

        /**
         * 获取用户名称。
         *
         * @return 用户名称
         */
        public String getUserName() {
            return this.userName;
        }

        /**
         * 设置用户名称。
         *
         * @param userName 用户名称
         */
        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
}
