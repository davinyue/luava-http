package org.rdlinux.luava.http;

/**
 * HTTP 响应读取或处理失败异常。
 */
public class HttpResponseConsumeException extends RuntimeException {
    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 HTTP 响应处理异常。
     *
     * @param cause 原始异常
     */
    public HttpResponseConsumeException(Throwable cause) {
        super(cause);
    }
}
