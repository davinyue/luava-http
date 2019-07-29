package org.linuxprobe.luava.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpClientConnectionManager;

import java.util.concurrent.TimeUnit;

/**
 * 无效连接回收
 */
@Slf4j
public class IdleConnectionEvictor {
    HttpClientConnectionManager connectionManager;
    private final long sleepTimeMs;
    private final long maxIdleTimeMs;
    private Thread thread;

    public IdleConnectionEvictor(HttpClientConnectionManager connectionManager, Long maxIdleTimeMs, Long sleepTimeMs) {
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

    private void initThread() {
        this.thread = new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(IdleConnectionEvictor.this.sleepTimeMs);
                        if (IdleConnectionEvictor.log.isInfoEnabled()) {
                            IdleConnectionEvictor.log.debug("idle connection evictor");
                        }
                        IdleConnectionEvictor.this.connectionManager.closeExpiredConnections();
                        if (IdleConnectionEvictor.this.maxIdleTimeMs > 0) {
                            IdleConnectionEvictor.this.connectionManager.closeIdleConnections(IdleConnectionEvictor.this.maxIdleTimeMs, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        IdleConnectionEvictor.log.error("", e);
                    }
                }
            }
        };
        this.thread.setDaemon(true);
    }

    public void start() {
        this.thread.start();
    }
}
