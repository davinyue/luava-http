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
    private final Integer connectTimeout;
    /**
     * 等待响应数据超时时间，单位毫秒。
     */
    private final Integer socketTimeout;
    /**
     * 从连接池获取连接超时时间，单位毫秒。
     */
    private final Integer connectionRequestTimeout;
    /**
     * 是否允许自动重定向。
     */
    private final Boolean redirectsEnabled;

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
     * 使用 Builder 创建可部分覆盖的单次请求配置。
     *
     * @return 单次请求配置 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取建立连接超时时间。
     *
     * @return 建立连接超时时间，单位毫秒；Builder 未设置时为 -1
     */
    public int getConnectTimeout() {
        return this.connectTimeout == null ? -1 : this.connectTimeout;
    }

    /**
     * 获取等待响应数据超时时间。
     *
     * @return 等待响应数据超时时间，单位毫秒；Builder 未设置时为 -1
     */
    public int getSocketTimeout() {
        return this.socketTimeout == null ? -1 : this.socketTimeout;
    }

    /**
     * 获取从连接池获取连接超时时间。
     *
     * @return 从连接池获取连接超时时间，单位毫秒；Builder 未设置时为 -1
     */
    public int getConnectionRequestTimeout() {
        return this.connectionRequestTimeout == null
                ? -1 : this.connectionRequestTimeout;
    }

    /**
     * 判断是否允许自动重定向。
     *
     * @return 是否允许自动重定向；Builder 未设置时为 false
     */
    public boolean isRedirectsEnabled() {
        return this.redirectsEnabled != null && this.redirectsEnabled;
    }

    /**
     * 判断是否显式配置建立连接超时。
     *
     * @return 是否显式配置建立连接超时
     */
    public boolean hasConnectTimeout() {
        return this.connectTimeout != null;
    }

    /**
     * 判断是否显式配置读取超时。
     *
     * @return 是否显式配置读取超时
     */
    public boolean hasSocketTimeout() {
        return this.socketTimeout != null;
    }

    /**
     * 判断是否显式配置连接池获取连接超时。
     *
     * @return 是否显式配置连接池获取连接超时
     */
    public boolean hasConnectionRequestTimeout() {
        return this.connectionRequestTimeout != null;
    }

    /**
     * 判断是否显式配置重定向策略。
     *
     * @return 是否显式配置重定向策略
     */
    public boolean hasRedirectsEnabled() {
        return this.redirectsEnabled != null;
    }

    /**
     * 单次请求配置 Builder。
     *
     * <p>未设置的配置项会继承当前请求工具的默认配置。</p>
     */
    public static final class Builder {
        /**
         * 建立连接超时时间。
         */
        private Integer connectTimeout;
        /**
         * 读取超时时间。
         */
        private Integer socketTimeout;
        /**
         * 连接池获取连接超时时间。
         */
        private Integer connectionRequestTimeout;
        /**
         * 是否允许自动重定向。
         */
        private Boolean redirectsEnabled;

        /**
         * 创建空的单次请求配置 Builder。
         */
        private Builder() {
        }

        /**
         * 设置建立连接超时时间。
         *
         * @param connectTimeout 建立连接超时时间，单位毫秒
         * @return 当前 Builder
         */
        public Builder connectTimeout(int connectTimeout) {
            validateTimeout(connectTimeout);
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * 设置读取超时时间。
         *
         * @param socketTimeout 读取超时时间，单位毫秒
         * @return 当前 Builder
         */
        public Builder socketTimeout(int socketTimeout) {
            validateTimeout(socketTimeout);
            this.socketTimeout = socketTimeout;
            return this;
        }

        /**
         * 设置连接池获取连接超时时间。
         *
         * @param connectionRequestTimeout 超时时间，单位毫秒
         * @return 当前 Builder
         */
        public Builder connectionRequestTimeout(
                int connectionRequestTimeout) {
            validateTimeout(connectionRequestTimeout);
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        /**
         * 设置是否允许自动重定向。
         *
         * @param redirectsEnabled 是否允许自动重定向
         * @return 当前 Builder
         */
        public Builder redirectsEnabled(boolean redirectsEnabled) {
            this.redirectsEnabled = redirectsEnabled;
            return this;
        }

        /**
         * 创建单次请求配置。
         *
         * @return 单次请求配置
         */
        public HttpRequestOptions build() {
            return new HttpRequestOptions(this);
        }

        /**
         * 校验超时时间。
         *
         * @param timeout 超时时间
         */
        private static void validateTimeout(int timeout) {
            if (timeout < 0) {
                throw new IllegalArgumentException(
                        "timeout must be greater than or equal to 0");
            }
        }
    }

    /**
     * 根据 Builder 创建单次请求配置。
     *
     * @param builder 单次请求配置 Builder
     */
    private HttpRequestOptions(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.socketTimeout = builder.socketTimeout;
        this.connectionRequestTimeout = builder.connectionRequestTimeout;
        this.redirectsEnabled = builder.redirectsEnabled;
    }
}
