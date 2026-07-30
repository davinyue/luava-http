package org.linuxprobe.luava;

import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.HttpResponseConsumeException;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP 响应消费和关闭测试。
 */
public class HttpResponseHandlingTest {
    /**
     * 验证响应读取失败时仍会关闭响应。
     */
    @Test
    public void shouldCloseResponseWhenReadingFails() {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }
        });
        TrackingResponse response = new TrackingResponse();
        response.setEntity(entity);

        try {
            HttpRequestUtils.consumeResponseAsString(response);
            Assert.fail("响应读取失败时应抛出处理异常");
        } catch (HttpResponseConsumeException expected) {
            Assert.assertEquals(
                    "read failed", expected.getCause().getMessage());
        }
        Assert.assertTrue(response.isClosed());
    }

    /**
     * 可记录关闭状态的 HTTP 响应。
     */
    private static final class TrackingResponse extends BasicHttpResponse
            implements CloseableHttpResponse {
        /**
         * 响应关闭状态。
         */
        private boolean closed;

        /**
         * 创建测试响应。
         */
        private TrackingResponse() {
            super(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
        }

        /**
         * 记录响应关闭。
         */
        @Override
        public void close() {
            this.closed = true;
        }

        /**
         * 获取响应关闭状态。
         *
         * @return 响应是否关闭
         */
        private boolean isClosed() {
            return this.closed;
        }
    }
}
