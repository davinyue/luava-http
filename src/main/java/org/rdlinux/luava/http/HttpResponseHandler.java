package org.rdlinux.luava.http;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

/**
 * HTTP 响应处理器。
 *
 * @param <T> 响应处理结果类型
 */
@FunctionalInterface
public interface HttpResponseHandler<T> {
    /**
     * 处理 HTTP 响应。
     *
     * <p>响应生命周期由调用该处理器的 {@link HttpRequestUtils} 管理，处理器不应
     * 主动关闭响应。</p>
     *
     * @param response HTTP 响应
     * @return 响应处理结果
     * @throws IOException 响应读取失败
     */
    T handle(CloseableHttpResponse response) throws IOException;
}
