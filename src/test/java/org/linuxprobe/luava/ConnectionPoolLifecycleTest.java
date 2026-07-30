package org.linuxprobe.luava;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.AsyncCloseableHttpClientBuilder;
import org.rdlinux.luava.http.CloseableHttpClientBuilder;
import org.rdlinux.luava.http.ConnectPool;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 连接池生命周期测试。
 */
public class ConnectionPoolLifecycleTest {
    private static final String SYNC_CLEANER_PREFIX = "http-idle-connection-cleaner-sync";
    private static final String ASYNC_CLEANER_PREFIX = "http-idle-connection-cleaner-async";

    /**
     * 验证请求工具复用客户端并在关闭后拒绝继续使用。
     *
     * @throws Exception 关闭客户端失败
     */
    @Test
    public void shouldReuseAndCloseDefaultHttpClient() throws Exception {
        HttpRequestUtils requestUtils = new HttpRequestUtils();
        CloseableHttpClient firstClient = requestUtils.getHttpClient();
        Assert.assertSame(firstClient, requestUtils.getHttpClient());
        requestUtils.close();
        try {
            requestUtils.getHttpClient();
            Assert.fail("关闭后不应继续获取HTTP客户端");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("http request utils already closed", expected.getMessage());
        }
    }

    /**
     * 验证同步连接清理线程随客户端关闭。
     *
     * @throws Exception 关闭客户端失败
     */
    @Test
    public void shouldStopSyncCleanerWhenClientClosed() throws Exception {
        long initialThreadCount = countThreads(SYNC_CLEANER_PREFIX);
        CloseableHttpClient client = new CloseableHttpClientBuilder()
                .setConnectPool(new ConnectPool().setCleanSleepTime(20))
                .build();
        awaitThreadCount(SYNC_CLEANER_PREFIX, initialThreadCount + 1);
        client.close();
        awaitThreadCount(SYNC_CLEANER_PREFIX, initialThreadCount);
    }

    /**
     * 验证同步客户端按照cleanSleepTime清理过期连接。
     *
     * @throws Exception 请求、反射或服务启动失败
     */
    @Test
    public void shouldUseConfiguredSyncCleanInterval() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/clean", exchange -> {
            byte[] responseBody = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        ConnectPool connectPool = new ConnectPool()
                .setKeepAliveDuration(20)
                .setMaxIdleTime(1000)
                .setCleanSleepTime(20);
        try (CloseableHttpClient client = new CloseableHttpClientBuilder()
                .setConnectPool(connectPool)
                .build()) {
            PoolingHttpClientConnectionManager connectionManager = getConnectionManager(client);
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/clean"))) {
                EntityUtils.consume(response.getEntity());
            }
            Assert.assertEquals(1, connectionManager.getTotalStats().getAvailable());
            awaitAvailableConnectionCount(connectionManager, 0);
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证异步连接清理线程只在客户端启动后运行，并随客户端关闭。
     *
     * @throws Exception 关闭客户端失败
     */
    @Test
    public void shouldStopAsyncCleanerWhenClientClosed() throws Exception {
        long initialThreadCount = countThreads(ASYNC_CLEANER_PREFIX);
        CloseableHttpAsyncClient client = new AsyncCloseableHttpClientBuilder()
                .setConnectPool(new ConnectPool().setCleanSleepTime(20).setIoThreadCount(1))
                .build();
        Assert.assertEquals(initialThreadCount, countThreads(ASYNC_CLEANER_PREFIX));
        client.start();
        awaitThreadCount(ASYNC_CLEANER_PREFIX, initialThreadCount + 1);
        client.close();
        awaitThreadCount(ASYNC_CLEANER_PREFIX, initialThreadCount);
    }

    /**
     * 验证同步客户端拒绝无法管理生命周期的共享连接管理器模式。
     */
    @Test
    public void shouldRejectSharedSyncConnectionManager() {
        try {
            new CloseableHttpClientBuilder()
                    .setConnectPool(new ConnectPool().setConnectionManagerShared(true))
                    .build();
            Assert.fail("同步客户端不应接受共享连接管理器");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("connectionManagerShared"));
        }
    }

    /**
     * 验证异步客户端拒绝无法管理生命周期的共享连接管理器模式。
     */
    @Test
    public void shouldRejectSharedAsyncConnectionManager() {
        try {
            new AsyncCloseableHttpClientBuilder()
                    .setConnectPool(new ConnectPool().setConnectionManagerShared(true))
                    .build();
            Assert.fail("异步客户端不应接受共享连接管理器");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("connectionManagerShared"));
        }
    }

    /**
     * 统计指定前缀的存活线程数量。
     *
     * @param threadNamePrefix 线程名称前缀
     * @return 存活线程数量
     */
    private long countThreads(String threadNamePrefix) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.isAlive() && thread.getName().startsWith(threadNamePrefix))
                .count();
    }

    /**
     * 等待线程数量达到预期值。
     *
     * @param threadNamePrefix 线程名称前缀
     * @param expectedCount 预期线程数量
     * @throws InterruptedException 等待被中断
     */
    private void awaitThreadCount(String threadNamePrefix, long expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (countThreads(threadNamePrefix) == expectedCount) {
                return;
            }
            Thread.sleep(10);
        }
        Assert.assertEquals(expectedCount, countThreads(threadNamePrefix));
    }

    /**
     * 获取客户端内部的连接管理器。
     *
     * @param client HTTP客户端
     * @return 连接管理器
     * @throws ReflectiveOperationException 无法读取连接管理器
     */
    private PoolingHttpClientConnectionManager getConnectionManager(CloseableHttpClient client)
            throws ReflectiveOperationException {
        Field connectionManagerField = client.getClass().getDeclaredField("connManager");
        connectionManagerField.setAccessible(true);
        return (PoolingHttpClientConnectionManager) connectionManagerField.get(client);
    }

    /**
     * 等待连接池可用连接数达到预期值。
     *
     * @param connectionManager 连接管理器
     * @param expectedCount 预期可用连接数
     * @throws InterruptedException 等待被中断
     */
    private void awaitAvailableConnectionCount(PoolingHttpClientConnectionManager connectionManager,
                                               int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (connectionManager.getTotalStats().getAvailable() == expectedCount) {
                return;
            }
            Thread.sleep(10);
        }
        Assert.assertEquals(expectedCount, connectionManager.getTotalStats().getAvailable());
    }
}
