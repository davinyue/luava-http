package org.rdlinux.luava.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定期清理连接池中过期和空闲连接的后台任务。
 */
final class IdleConnectionCleaner implements Closeable {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(IdleConnectionCleaner.class);
    /**
     * 清理线程序号
     */
    private static final AtomicInteger order = new AtomicInteger(1);
    /**
     * 连接管理器适配器
     */
    private final ConnectionManager connectionManager;
    /**
     * 连接最大空闲时间
     */
    private final long maxIdleTimeMs;
    /**
     * 清理间隔
     */
    private final long sleepTimeMs;
    /**
     * 启动状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    /**
     * 关闭状态
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    /**
     * 后台清理线程
     */
    private final Thread thread;

    /**
     * 创建连接清理器。
     *
     * @param threadNamePrefix 线程名称前缀
     * @param connectionManager 连接管理器适配器
     * @param maxIdleTimeMs 连接最大空闲时间，单位毫秒
     * @param sleepTimeMs 清理间隔，单位毫秒
     */
    IdleConnectionCleaner(String threadNamePrefix, ConnectionManager connectionManager,
                          long maxIdleTimeMs, long sleepTimeMs) {
        if (threadNamePrefix == null || threadNamePrefix.isEmpty()) {
            throw new IllegalArgumentException("threadNamePrefix can not be empty");
        }
        if (connectionManager == null) {
            throw new IllegalArgumentException("connectionManager can not be null");
        }
        if (maxIdleTimeMs <= 0) {
            throw new IllegalArgumentException("maxIdleTimeMs must be greater than 0");
        }
        if (sleepTimeMs <= 0) {
            throw new IllegalArgumentException("sleepTimeMs must be greater than 0");
        }
        this.connectionManager = connectionManager;
        this.maxIdleTimeMs = maxIdleTimeMs;
        this.sleepTimeMs = sleepTimeMs;
        this.thread = new Thread(this::cleanConnections,
                threadNamePrefix + "-" + order.getAndIncrement());
        this.thread.setDaemon(true);
    }

    /**
     * 启动连接清理任务，重复调用不会重复启动线程。
     */
    void start() {
        if (this.closed.get()) {
            throw new IllegalStateException("idle connection cleaner already closed");
        }
        if (this.started.compareAndSet(false, true)) {
            this.thread.start();
        }
    }

    /**
     * 停止连接清理任务并等待线程退出。
     */
    @Override
    public void close() {
        if (this.closed.compareAndSet(false, true) && this.started.get()) {
            this.thread.interrupt();
            try {
                this.thread.join(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 执行周期性连接清理。
     */
    private void cleanConnections() {
        while (!this.closed.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(this.sleepTimeMs);
                if (this.closed.get()) {
                    return;
                }
                this.connectionManager.closeExpiredConnections();
                this.connectionManager.closeIdleConnections(this.maxIdleTimeMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                log.warn("清理空闲连接失败", e);
            }
        }
    }

    /**
     * 屏蔽同步和异步连接管理器类型差异的清理接口。
     */
    interface ConnectionManager {
        /**
         * 关闭已过期连接。
         */
        void closeExpiredConnections();

        /**
         * 关闭超过指定空闲时间的连接。
         *
         * @param idleTime 空闲时间
         * @param timeUnit 时间单位
         */
        void closeIdleConnections(long idleTime, TimeUnit timeUnit);
    }
}
