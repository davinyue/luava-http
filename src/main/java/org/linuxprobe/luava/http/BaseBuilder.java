package org.linuxprobe.luava.http;

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

public class BaseBuilder {
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
