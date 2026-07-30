package org.rdlinux.luava.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * 同步和异步HTTP客户端构建器的公共配置能力。
 */
public class BaseBuilder {
    /**
     * 校验连接池配置。
     *
     * @param connectPool 连接池配置
     */
    protected void validateConnectPool(ConnectPool connectPool) {
        if (connectPool == null) {
            throw new IllegalArgumentException("connectPool can not be null");
        }
        if (connectPool.getConnectTimeout() < 0
                || connectPool.getSocketTimeout() < 0
                || connectPool.getConnectionRequestTimeout() < 0) {
            throw new IllegalArgumentException("timeout must be greater than or equal to 0");
        }
        if (connectPool.getSingleMaxActive() <= 0 || connectPool.getAllMaxActive() <= 0) {
            throw new IllegalArgumentException("max active connections must be greater than 0");
        }
        if (connectPool.getAllMaxActive() < connectPool.getSingleMaxActive()) {
            throw new IllegalArgumentException("allMaxActive must be greater than or equal to singleMaxActive");
        }
        if (connectPool.getMaxIdleTime() <= 0
                || connectPool.getCleanSleepTime() <= 0
                || connectPool.getKeepAliveDuration() <= 0) {
            throw new IllegalArgumentException("connection lifecycle time must be greater than 0");
        }
        if (connectPool.getRetryCount() < 0) {
            throw new IllegalArgumentException("retryCount must be greater than or equal to 0");
        }
        if (connectPool.getIoThreadCount() <= 0) {
            throw new IllegalArgumentException("ioThreadCount must be greater than 0");
        }
    }

    protected SSLContext createSSLContext() {
        // 这里设置信任所有证书
        SSLContext sslContext;
        try {
            // 信任所有
            sslContext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        return sslContext;
    }

    /**
     * 创建Http请求配置参数
     */
    protected RequestConfig createRequestConfig(ConnectPool connectPool) {
        return RequestConfig.custom()
                // 连接超时时间
                .setConnectTimeout(connectPool.getConnectTimeout())
                // 读超时时间（等待数据超时时间
                .setSocketTimeout(connectPool.getSocketTimeout())
                // 从池中获取连接超时时间
                .setConnectionRequestTimeout(connectPool.getConnectionRequestTimeout()).build();
    }

    protected ConnectionKeepAliveStrategy createConnectionKeepAliveStrategy(ConnectPool connectPool) {
        return new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                long result = super.getKeepAliveDuration(response, context);
                if (result <= 0) {
                    result = connectPool.getKeepAliveDuration();
                }
                return result;
            }
        };
    }
}
