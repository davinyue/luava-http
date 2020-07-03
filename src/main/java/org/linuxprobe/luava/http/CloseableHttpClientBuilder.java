package org.linuxprobe.luava.http;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class CloseableHttpClientBuilder extends BaseBuilder {
    public CloseableHttpClientBuilder() {
    }

    private ConnectPool connectPool;

    /**
     * 创建连接池管理
     */
    private HttpClientConnectionManager createClientConnectionManager(ConnectPool connectPool) {
        PoolingHttpClientConnectionManager clientConnectionManager;
        SSLConnectionSocketFactory sslSf = new SSLConnectionSocketFactory(this.createSSLContext(),
                NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslSf).register("http", PlainConnectionSocketFactory.getSocketFactory())
                .build();
        // 配置连接池
        clientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 最大连接
        clientConnectionManager.setMaxTotal(connectPool.getAllMaxActive());
        // 默认的每个路由的最大连接数
        clientConnectionManager.setDefaultMaxPerRoute(connectPool.getSingleMaxActive());
        // 设置路由的最大连接数，会覆盖defaultMaxPerRoute
        // clientConnectionManager.setMaxPerRoute(new HttpRoute(new
        // HttpHost("https://www.linuxprobe.org", 443)), 10);
        SocketConfig socketConfig = SocketConfig.custom()
                // 是否立即发送数据，设置为true会关闭Socket缓冲，默认为false
                .setTcpNoDelay(true)
                // 是否可以在一个进程关闭Socket后，即使它还没有释放端口，其它进程还可以立即重用端口
                .setSoReuseAddress(true)
                // 接收数据的等待超时时间，单位ms
                .setSoTimeout(connectPool.getSocketTimeout())
                // 关闭Socket时，要么发送完所有数据，要么等待60s后，就关闭连接，此时socket.close()是阻塞的
                .setSoLinger(6)
                // 开启监视TCP连接是否有效
                .setSoKeepAlive(true).build();
        // 默认路由的socket配置
        clientConnectionManager.setDefaultSocketConfig(socketConfig);
        // 针对特定路由的socket配置
        // clientConnectionManager.setSocketConfig(new HttpHost("somehost", 80),
        // socketConfig);
        return clientConnectionManager;
    }

    /**
     * 创建重试策略
     */
    private HttpRequestRetryHandler createHttpRequestRetryHandler(ConnectPool connectPool) {
        // 自定义重试策略
        return (exception, executionCount, context) -> {
            // 如果重试次数大于等于规定次数
            if (executionCount >= connectPool.getRetryCount()) {
                return false;
            }
            // 如果服务器丢掉了连接，那么就重试
            else if (exception instanceof NoHttpResponseException) {
                return true;
            }
            // 不要重试SSL握手异常
            else if (exception instanceof SSLHandshakeException) {
                return false;
            }
            // io中断
            else if (exception instanceof InterruptedIOException) {
                return false;
            }
            // 目标服务器不可达
            else if (exception instanceof UnknownHostException) {
                return false;
            }
            // SSL握手异常
            else if (exception instanceof SSLException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // Retry if the request is considered idempotent
            // 如果请求类型不是HttpEntityEnclosingRequest，被认为是幂等的，那么就重试
            // HttpEntityEnclosingRequest指的是有请求体的request，比HttpRequest多一个Entity属性
            // 而常用的GET请求是没有请求体的，POST、PUT都是有请求体的 Rest一般用GET请求获取数据，故幂等，POST用于新增数据，故不幂等
            return !(request instanceof HttpEntityEnclosingRequest);
        };
    }

    private CloseableHttpClient getHttpClient() {
        if (this.connectPool == null) {
            return HttpClients.createDefault();
        } else {
            return HttpClients.custom()
                    // 配置连接池管理对象
                    .setConnectionManager(this.createClientConnectionManager(this.connectPool))
                    // 设置保持长连接策略
                    .setKeepAliveStrategy(this.createConnectionKeepAliveStrategy(this.connectPool))
                    .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
                    // 默认请求配置
                    .setDefaultRequestConfig(this.createRequestConfig(this.connectPool))
                    // 重试策略
                    .setRetryHandler(this.createHttpRequestRetryHandler(this.connectPool))
                    // If you set it to true the client won't close the connection manager
                    .setConnectionManagerShared(this.connectPool.getConnectionManagerShared())
                    // 空闲和无效连接释放
                    .evictIdleConnections(this.connectPool.getMaxIdleTime(), TimeUnit.MILLISECONDS).build();
        }
    }

    public CloseableHttpClientBuilder setConnectPool(ConnectPool connectPool) {
        this.connectPool = connectPool;
        return this;
    }

    /**
     * 构建连接池
     */
    public CloseableHttpClient build() {
        return this.getHttpClient();
    }
}
