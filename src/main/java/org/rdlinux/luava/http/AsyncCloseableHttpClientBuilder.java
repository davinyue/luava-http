package org.rdlinux.luava.http;

import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步HTTP客户端构建器。
 */
public class AsyncCloseableHttpClientBuilder extends BaseBuilder {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(AsyncCloseableHttpClientBuilder.class);
    /**
     * 连接池配置
     */
    private ConnectPool connectPool;
    /**
     * 是否显式信任全部TLS证书并关闭主机名校验。
     */
    private boolean insecureTrustAllCertificates;

    /**
     * 创建异步连接池管理
     */
    private PoolingNHttpClientConnectionManager createAsyncClientConnectionManager() {
        PoolingNHttpClientConnectionManager clientConnectionManager;
        //配置io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                setIoThreadCount(this.connectPool.getIoThreadCount())
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
        Registry<SchemeIOSessionStrategy> ioSessionStrategyRegistry =
                RegistryBuilder.<SchemeIOSessionStrategy>create()
                        .register("http", NoopIOSessionStrategy.INSTANCE)
                        .register("https", this.insecureTrustAllCertificates
                                ? new SSLIOSessionStrategy(
                                this.createInsecureSSLContext(),
                                NoopHostnameVerifier.INSTANCE)
                                : SSLIOSessionStrategy.getSystemDefaultStrategy())
                        .build();
        clientConnectionManager = new PoolingNHttpClientConnectionManager(
                ioReactor, ioSessionStrategyRegistry);
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
        return clientConnectionManager;
    }

    private CloseableHttpAsyncClient getAsyncHttpClient() {
        if (this.connectPool == null) {
            if (!this.insecureTrustAllCertificates) {
                return HttpAsyncClients.createDefault();
            }
            return HttpAsyncClients.custom()
                    .setSSLContext(this.createInsecureSSLContext())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } else {
            this.validateConnectPool(this.connectPool);
            PoolingNHttpClientConnectionManager asyncClientConnectionManager = this.createAsyncClientConnectionManager();
            CloseableHttpAsyncClient delegate = HttpAsyncClients.custom()
                    // 配置连接池管理对象
                    .setConnectionManager(asyncClientConnectionManager)
                    // 设置保持长连接策略
                    .setKeepAliveStrategy(this.createConnectionKeepAliveStrategy(this.connectPool))
                    .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
                    // 默认请求配置
                    .setDefaultRequestConfig(this.createRequestConfig(this.connectPool))
                    .build();
            IdleConnectionCleaner connectionCleaner = new IdleConnectionCleaner(
                    "http-idle-connection-cleaner-async",
                    new IdleConnectionCleaner.ConnectionManager() {
                        @Override
                        public void closeExpiredConnections() {
                            asyncClientConnectionManager.closeExpiredConnections();
                        }

                        @Override
                        public void closeIdleConnections(long idleTime, TimeUnit timeUnit) {
                            asyncClientConnectionManager.closeIdleConnections(idleTime, timeUnit);
                        }
                    },
                    this.connectPool.getMaxIdleTime(),
                    this.connectPool.getCleanSleepTime());
            return new OwnedCloseableHttpAsyncClient(delegate, asyncClientConnectionManager, connectionCleaner);
        }
    }

    /**
     * 设置连接池配置。
     *
     * @param connectPool 连接池配置
     * @return 当前构建器
     */
    public AsyncCloseableHttpClientBuilder setConnectPool(ConnectPool connectPool) {
        this.connectPool = connectPool;
        return this;
    }

    /**
     * 设置是否信任全部TLS证书并关闭主机名校验。
     *
     * <p>该选项会降低HTTPS安全性，只应用于明确接受风险的兼容场景。</p>
     *
     * @param insecureTrustAllCertificates 是否启用不安全TLS兼容模式
     * @return 当前构建器
     */
    public AsyncCloseableHttpClientBuilder setInsecureTrustAllCertificates(
            boolean insecureTrustAllCertificates) {
        this.insecureTrustAllCertificates = insecureTrustAllCertificates;
        return this;
    }

    /**
     * 构建连接池
     *
     * @return 异步HTTP客户端
     */
    public CloseableHttpAsyncClient build() {
        return this.getAsyncHttpClient();
    }

    /**
     * 管理异步客户端、连接池和清理线程共同生命周期的客户端。
     */
    private static class OwnedCloseableHttpAsyncClient extends CloseableHttpAsyncClient {
        /**
         * Apache异步客户端
         */
        private final CloseableHttpAsyncClient delegate;
        /**
         * 异步连接管理器
         */
        private final PoolingNHttpClientConnectionManager connectionManager;
        /**
         * 空闲连接清理器
         */
        private final IdleConnectionCleaner connectionCleaner;
        /**
         * 客户端关闭状态
         */
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * 创建管理完整生命周期的异步客户端。
         *
         * @param delegate Apache异步客户端
         * @param connectionManager 连接管理器
         * @param connectionCleaner 空闲连接清理器
         */
        private OwnedCloseableHttpAsyncClient(CloseableHttpAsyncClient delegate,
                                              PoolingNHttpClientConnectionManager connectionManager,
                                              IdleConnectionCleaner connectionCleaner) {
            this.delegate = delegate;
            this.connectionManager = connectionManager;
            this.connectionCleaner = connectionCleaner;
        }

        /**
         * 判断异步客户端是否运行。
         *
         * @return 运行状态
         */
        @Override
        public boolean isRunning() {
            return this.delegate.isRunning();
        }

        /**
         * 启动异步客户端和连接清理器。
         */
        @Override
        public synchronized void start() {
            if (this.closed.get()) {
                throw new IllegalStateException("http async client already closed");
            }
            this.delegate.start();
            this.connectionCleaner.start();
        }

        /**
         * 执行异步请求。
         *
         * @param requestProducer 请求生产器
         * @param responseConsumer 响应消费者
         * @param context HTTP上下文
         * @param callback 完成回调
         * @param <T> 响应类型
         * @return 异步执行结果
         */
        @Override
        public <T> Future<T> execute(HttpAsyncRequestProducer requestProducer,
                                     HttpAsyncResponseConsumer<T> responseConsumer,
                                     HttpContext context,
                                     FutureCallback<T> callback) {
            return this.delegate.execute(requestProducer, responseConsumer, context, callback);
        }

        /**
         * 关闭清理线程、异步客户端和连接管理器。
         *
         * @throws IOException 关闭连接管理器失败
         */
        @Override
        public synchronized void close() throws IOException {
            if (!this.closed.compareAndSet(false, true)) {
                return;
            }
            this.connectionCleaner.close();
            IOException closeException = null;
            try {
                this.delegate.close();
            } catch (IOException e) {
                closeException = e;
            }
            try {
                this.connectionManager.shutdown();
            } catch (IOException e) {
                if (closeException == null) {
                    closeException = e;
                } else {
                    closeException.addSuppressed(e);
                }
            }
            if (closeException != null) {
                throw closeException;
            }
        }
    }

    /**
     * 兼容旧版公开API的异步连接清理器。
     *
     * @deprecated 请由构建后的异步客户端统一管理连接清理任务生命周期
     */
    @Deprecated
    public static class IdleConnectionRecover implements Closeable {
        /**
         * 空闲连接清理器
         */
        private final IdleConnectionCleaner connectionCleaner;

        /**
         * 创建异步连接清理器。
         *
         * @param connectionManager 异步连接管理器
         * @param maxIdleTimeMs 连接最大空闲时间，单位毫秒
         * @param sleepTimeMs 清理间隔，单位毫秒
         */
        public IdleConnectionRecover(NHttpClientConnectionManager connectionManager,
                                     Long maxIdleTimeMs,
                                     Long sleepTimeMs) {
            if (connectionManager == null) {
                throw new IllegalArgumentException("connectionManager can not be null");
            }
            long actualMaxIdleTimeMs = maxIdleTimeMs == null || maxIdleTimeMs <= 0
                    ? 1800000L : maxIdleTimeMs;
            long actualSleepTimeMs = sleepTimeMs == null || sleepTimeMs <= 0
                    ? 30000L : sleepTimeMs;
            this.connectionCleaner = new IdleConnectionCleaner(
                    "http-idle-connection-cleaner-async-compatible",
                    new IdleConnectionCleaner.ConnectionManager() {
                        @Override
                        public void closeExpiredConnections() {
                            connectionManager.closeExpiredConnections();
                        }

                        @Override
                        public void closeIdleConnections(long idleTime, TimeUnit timeUnit) {
                            connectionManager.closeIdleConnections(idleTime, timeUnit);
                        }
                    },
                    actualMaxIdleTimeMs,
                    actualSleepTimeMs);
        }

        /**
         * 获取异常堆栈文本。
         *
         * @param throwable 异常
         * @return 异常堆栈文本
         */
        public static String getStackTrace(Throwable throwable) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            printWriter.close();
            return stringWriter.toString();
        }

        /**
         * 启动连接清理任务。
         */
        public void start() {
            this.connectionCleaner.start();
        }

        /**
         * 停止连接清理任务。
         */
        @Override
        public void close() {
            this.connectionCleaner.close();
        }
    }
}
