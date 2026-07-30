package org.rdlinux.luava.http;

import java.io.IOException;

/**
 * HTTP请求执行异常。
 *
 * <p>异常携带客户端侧请求正文写出状态，调用方仍需结合远程接口的幂等和状态查询
 * 能力判断是否可以重试。</p>
 */
public class HttpRequestExecuteException extends RuntimeException {
    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 请求正文写出状态。
     */
    private final RequestWriteState requestWriteState;

    /**
     * 创建HTTP请求执行异常。
     *
     * @param cause 原始IO异常
     * @param requestWriteState 请求正文写出状态
     */
    public HttpRequestExecuteException(IOException cause, RequestWriteState requestWriteState) {
        super(cause);
        if (requestWriteState == null) {
            throw new IllegalArgumentException("requestWriteState can not be null");
        }
        this.requestWriteState = requestWriteState;
    }

    /**
     * 获取请求正文写出状态。
     *
     * @return 请求正文写出状态
     */
    public RequestWriteState getRequestWriteState() {
        return this.requestWriteState;
    }
}
