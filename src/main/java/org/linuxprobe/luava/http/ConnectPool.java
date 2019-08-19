package org.linuxprobe.luava.http;

import lombok.Getter;
import lombok.Setter;

/**
 * 连接池
 */
@Setter
@Getter
public class ConnectPool {
    private static final int defaultConnectTimeout = 5000;
    private static final int defaultSocketTimeout = 10000;
    private static final int defaultConnectionRequestTimeout = 20000;
    private static final int defaultSingleMaxActive = 5;
    private static final int defaultAllMaxActive = 40;
    /**
     * 连接池默认清理空闲30分钟的连接
     */
    private static final long defaultMaxLifetime = 1800000;
    /**
     * 连接池默认清理间隔时间30秒
     */
    private static final long defaultCleanSleepTimeMs = 30000;
    /**
     * 连接超时时间,单位毫秒,默认5秒
     */
    private int connectTimeout = defaultConnectTimeout;
    /**
     * 读取超时时间,单位毫秒,默认10秒
     */
    private int socketTimeout = defaultSocketTimeout;
    /**
     * 从池中获取连接超时时间,单位毫秒,默认20秒
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
     * 连最多高存活时间, 单位毫秒, 默认30分钟
     */
    private long maxLifetime = defaultMaxLifetime;
    /**
     * 连接池默认清理间隔时间30秒
     */
    private long cleanSleepTimeMs = defaultCleanSleepTimeMs;
}
