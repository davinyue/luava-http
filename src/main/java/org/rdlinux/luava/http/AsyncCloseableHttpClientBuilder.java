package org.rdlinux.luava.http;

import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncCloseableHttpClientBuilder extends BaseBuilder {
    private static final Logger log = LoggerFactory.getLogger(AsyncCloseableHttpClientBuilder.class);
    private ConnectPool connectPool;

    /**
     * 创建异步连接池管理
     */
    private PoolingNHttpClientConnectionManager createAsyncClientConnectionManager() {
        PoolingNHttpClientConnectionManager clientConnectionManager;
        //配置io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2)
                .setSoKeepAlive(true)
                .build();
        //设置连接池大小
        DefaultConnectingIOReactor ioReactor;
        try {
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig, new NamedThreadFactory("async-http-client"));
        } catch (IOReactorException e) {
            throw new RuntimeException(e);
        }
        ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
            @Override
            public boolean handle(IOException ex) {
                AsyncCloseableHttpClientBuilder.log.warn("IOReactor encountered a checked exception", ex);
                return true; // Return true to note this exception as handled, it will not be re-thrown
            }

            @Override
            public boolean handle(RuntimeException ex) {
                AsyncCloseableHttpClientBuilder.log.warn("IOReactor encountered a runtime exception", ex);
                return true; // Return true to note this exception as handled, it will not be re-thrown
            }
        });
        // 配置连接池
        clientConnectionManager = new PoolingNHttpClientConnectionManager(ioReactor);
        // 最大连接
        clientConnectionManager.setMaxTotal(this.connectPool.getAllMaxActive());
        // 默认的每个路由的最大连接数
        clientConnectionManager.setDefaultMaxPerRoute(this.connectPool.getSingleMaxActive());
        // 设置路由的最大连接数，会覆盖defaultMaxPerRoute
        // clientConnectionManager.setMaxPerRoute(new HttpRoute(new
        // HttpHost("https://www.linuxprobe.org", 443)), 10);
        ConnectionConfig connectionConfig = ConnectionConfig.custom().build();
        // 默认路由的socket配置
        clientConnectionManager.setDefaultConnectionConfig(connectionConfig);
        // 针对特定路由的socket配置
        // clientConnectionManager.setSocketConfig(new HttpHost("somehost", 80),
        // socketConfig);
        clientConnectionManager.closeIdleConnections(this.connectPool.getMaxIdleTime(), TimeUnit.MILLISECONDS);
        return clientConnectionManager;
    }

    private CloseableHttpAsyncClient getAsyncHttpClient() {
        if (this.connectPool == null) {
            return HttpAsyncClients.createDefault();
        } else {
            PoolingNHttpClientConnectionManager asyncClientConnectionManager = this.createAsyncClientConnectionManager();
            CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(this.createSSLContext())
                    // 配置连接池管理对象
                    .setConnectionManager(asyncClientConnectionManager)
                    // 设置保持长连接策略
                    .setKeepAliveStrategy(this.createConnectionKeepAliveStrategy(this.connectPool))
                    .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
                    // 默认请求配置
                    .setDefaultRequestConfig(this.createRequestConfig(this.connectPool))
                    // If you set it to true the client won't close the connection manager
                    .setConnectionManagerShared(this.connectPool.getConnectionManagerShared()).build();
            IdleConnectionRecover idleConnectionRecover = new IdleConnectionRecover(asyncClientConnectionManager,
                    this.connectPool.getMaxIdleTime(), this.connectPool.getCleanSleepTime());
            idleConnectionRecover.start();
            return client;
        }
    }

    public AsyncCloseableHttpClientBuilder setConnectPool(ConnectPool connectPool) {
        this.connectPool = connectPool;
        return this;
    }

    /**
     * 构建连接池
     */
    public CloseableHttpAsyncClient build() {
        return this.getAsyncHttpClient();
    }

    /**
     * 无效连接回收
     */
    public static class IdleConnectionRecover {
        private static final Logger log = LoggerFactory.getLogger(IdleConnectionRecover.class);
        private static final AtomicInteger order = new AtomicInteger(1);
        private final NHttpClientConnectionManager connectionManager;
        private final long sleepTimeMs;
        private final long maxIdleTimeMs;
        private Thread thread;

        public IdleConnectionRecover(NHttpClientConnectionManager connectionManager, Long maxIdleTimeMs, Long sleepTimeMs) {
            if (connectionManager == null) {
                throw new IllegalArgumentException("connectionManager can not be null");
            }
            if (maxIdleTimeMs == null || maxIdleTimeMs <= 0) {
                maxIdleTimeMs = 1800000L;
            }
            if (sleepTimeMs == null || sleepTimeMs <= 0) {
                sleepTimeMs = 30000L;
            }
            this.maxIdleTimeMs = maxIdleTimeMs;
            this.sleepTimeMs = sleepTimeMs;
            this.connectionManager = connectionManager;
            this.initThread();
        }

        public static String getStackTrace(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            return sw.toString();
        }

        private void initThread() {
            this.thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(this.sleepTimeMs);
                        if (log.isInfoEnabled()) {
                            log.debug("idle connection recover");
                        }
                        this.connectionManager.closeExpiredConnections();
                        if (this.maxIdleTimeMs > 0) {
                            this.connectionManager.closeIdleConnections(this.maxIdleTimeMs, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("http-idle-connection-recover-error, msg:{}", getStackTrace(e));
                    }
                }
            });
            this.thread.setDaemon(true);
            this.thread.setName("http-idle-connection-recover-" + order.getAndIncrement());
        }

        public void start() {
            this.thread.start();
        }
    }
}
