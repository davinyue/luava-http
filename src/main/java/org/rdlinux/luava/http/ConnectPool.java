package org.rdlinux.luava.http;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 连接池配置
 */
@Setter
@Getter
@Accessors(chain = true)
public class ConnectPool {
    private static final int defaultConnectTimeout = 5000;
    private static final int defaultSocketTimeout = 10000;
    private static final int defaultConnectionRequestTimeout = 10000;
    private static final int defaultSingleMaxActive = 5;
    private static final int defaultAllMaxActive = 40;
    private static final boolean defaultConnectionManagerShared = false;
    /**
     * 默认长连接持续时间60000, 单位毫秒
     */
    private static final long defaultKeepAliveDuration = 60000;
    /**
     * 连接最大空闲时间, 默认30分钟, 单位毫秒
     */
    private static final long defaultMaxIdleTime = 1800000;
    /**
     * 连接池默认清理间隔时间, 默认30秒, 单位毫秒
     */
    private static final long defaultCleanSleepTime = 30000;
    /**
     * 默认失败重试次数
     */
    private static final int defaultRetryCount = 3;
    /**
     * 连接超时时间, 默认5秒, 单位毫秒
     */
    private int connectTimeout = defaultConnectTimeout;
    /**
     * 读取超时时间, 默认10秒, 单位毫秒
     */
    private int socketTimeout = defaultSocketTimeout;
    /**
     * 从池中获取连接超时时间, 默认10秒, 单位毫秒
     */
    private int connectionRequestTimeout = defaultConnectionRequestTimeout;
    /**
     * 单个路由最大活跃连接数,默认5
     */
    private int singleMaxActive = defaultSingleMaxActive;
    /**
     * 所有路由最大总活跃连接数,默认40,在队列服务器和主题服务器是分开的情况下,建议配置为singleMaxActive的两倍,如果
     * 不是分开的,建议与singleMaxActive保持一致
     */
    private int allMaxActive = defaultAllMaxActive;
    /**
     * 连接最大空闲时间, 默认30分钟, 单位毫秒,
     */
    private long maxIdleTime = defaultMaxIdleTime;
    /**
     * 连接池默认清理间隔时间, 默认30秒, 单位毫秒
     */
    private long cleanSleepTime = defaultCleanSleepTime;
    /**
     * 长连接可持续时间, 默认为60秒, 单位毫秒, 在服务器不返回长连接持续时间时, 将使用该属性
     */
    private long keepAliveDuration = defaultKeepAliveDuration;
    /**
     * 是否共享连接池管理工具, 默认false ,如果开启, 将不能通过CloseableHttpClient关闭连接池管理工具;<br/>
     * 使用RestTemplate时建议开启, 关闭时可能会出现出现 connection pool shut down 的异常,
     */
    private Boolean connectionManagerShared = defaultConnectionManagerShared;
    /**
     * 失败重试次数, 默认3
     */
    private int retryCount = defaultRetryCount;
}
