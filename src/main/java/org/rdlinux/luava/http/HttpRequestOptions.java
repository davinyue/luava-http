package org.rdlinux.luava.http;

/**
 * 单次HTTP请求配置。
 *
 * <p>该配置只影响当前请求，不会修改共享HTTP客户端或连接池的默认配置。</p>
 */
public final class HttpRequestOptions {
    /**
     * 建立连接超时时间，单位毫秒。
     */
    private final int connectTimeout;
    /**
     * 等待响应数据超时时间，单位毫秒。
     */
    private final int socketTimeout;
    /**
     * 从连接池获取连接超时时间，单位毫秒。
     */
    private final int connectionRequestTimeout;
    /**
     * 是否允许自动重定向。
     */
    private final boolean redirectsEnabled;

    /**
     * 创建单次HTTP请求配置。
     *
     * @param connectTimeout 建立连接超时时间，单位毫秒
     * @param socketTimeout 等待响应数据超时时间，单位毫秒
     * @param connectionRequestTimeout 从连接池获取连接超时时间，单位毫秒
     * @param redirectsEnabled 是否允许自动重定向
     */
    public HttpRequestOptions(int connectTimeout, int socketTimeout,
                              int connectionRequestTimeout, boolean redirectsEnabled) {
        if (connectTimeout < 0 || socketTimeout < 0 || connectionRequestTimeout < 0) {
            throw new IllegalArgumentException("timeout must be greater than or equal to 0");
        }
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.redirectsEnabled = redirectsEnabled;
    }

    /**
     * 获取建立连接超时时间。
     *
     * @return 建立连接超时时间，单位毫秒
     */
    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    /**
     * 获取等待响应数据超时时间。
     *
     * @return 等待响应数据超时时间，单位毫秒
     */
    public int getSocketTimeout() {
        return this.socketTimeout;
    }

    /**
     * 获取从连接池获取连接超时时间。
     *
     * @return 从连接池获取连接超时时间，单位毫秒
     */
    public int getConnectionRequestTimeout() {
        return this.connectionRequestTimeout;
    }

    /**
     * 判断是否允许自动重定向。
     *
     * @return 是否允许自动重定向
     */
    public boolean isRedirectsEnabled() {
        return this.redirectsEnabled;
    }
}
