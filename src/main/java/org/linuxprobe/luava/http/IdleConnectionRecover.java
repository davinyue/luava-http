package org.linuxprobe.luava.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpClientConnectionManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 无效连接回收
 */
@Slf4j
public class IdleConnectionRecover {
    private static final AtomicInteger order = new AtomicInteger(1);
    private HttpClientConnectionManager connectionManager;
    private final long sleepTimeMs;
    private final long maxIdleTimeMs;
    private Thread thread;

    public IdleConnectionRecover(HttpClientConnectionManager connectionManager, Long maxIdleTimeMs, Long sleepTimeMs) {
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

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    private void initThread() {
        this.thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(IdleConnectionRecover.this.sleepTimeMs);
                    if (IdleConnectionRecover.log.isInfoEnabled()) {
                        IdleConnectionRecover.log.debug("idle connection recover");
                    }
                    IdleConnectionRecover.this.connectionManager.closeExpiredConnections();
                    if (IdleConnectionRecover.this.maxIdleTimeMs > 0) {
                        IdleConnectionRecover.this.connectionManager.closeIdleConnections(IdleConnectionRecover.this.maxIdleTimeMs, TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    IdleConnectionRecover.log.error("http-idle-connection-recover-error, msg:{}", getStackTrace(e));
                }
            }
        });
        this.thread.setDaemon(true);
        this.thread.setName("http-idle-connection-recover-" + order.getAndIncrement());
    }

    public void start() {
        this.thread.start();
    }
}
