package org.linuxprobe.luava;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.ConnectPool;
import org.rdlinux.luava.http.HttpRequestExecuteException;
import org.rdlinux.luava.http.HttpRequestOptions;
import org.rdlinux.luava.http.HttpRequestUtils;
import org.rdlinux.luava.http.RequestWriteState;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

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
             CloseableHttpResponse response = requestUtils.postRequest(
                     "http://127.0.0.1:" + server.getAddress().getPort() + "/request", "{}")) {
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
                         requestUtils.httpRequestWithOptions(
                                 HttpGet.METHOD_NAME, redirectUrl,
                                 null, null, false, noRedirect)) {
                Assert.assertEquals(
                        302, response.getStatusLine().getStatusCode());
            }

            HttpRequestOptions followRedirect = new HttpRequestOptions(
                    1000, 1000, 1000, true);
            try (CloseableHttpResponse response =
                         requestUtils.httpRequestWithOptions(
                                 HttpGet.METHOD_NAME, redirectUrl,
                                 null, null, false, followRedirect)) {
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
                requestUtils.httpRequestWithOptions(
                        HttpGet.METHOD_NAME, url,
                        null, null, false, options);
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
                requestUtils.getRequest(url);
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
                requestUtils.postRequest(
                        "http://127.0.0.1:" + unavailablePort + "/request",
                        "{}");
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
                requestUtils.httpRequestWithOptions(
                        HttpPost.METHOD_NAME,
                        "http://127.0.0.1:"
                                + server.getAddress().getPort() + "/drop",
                        null, "request-body", false,
                        new HttpRequestOptions(
                                500, 500, 500, false));
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
}
