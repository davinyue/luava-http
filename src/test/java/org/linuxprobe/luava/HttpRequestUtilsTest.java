package org.linuxprobe.luava;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * HTTP请求工具测试。
 */
public class HttpRequestUtilsTest {

    /**
     * 验证请求响应和客户端均可正常关闭。
     *
     * @throws Exception 请求或服务启动失败
     */
    @Test
    public void requestTest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/request", exchange -> {
            byte[] responseBody = "success".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        try (HttpRequestUtils requestUtils = new HttpRequestUtils();
             CloseableHttpResponse response = requestUtils.postRequest(
                     "http://127.0.0.1:" + server.getAddress().getPort() + "/request", "{}")) {
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertEquals("success", EntityUtils.toString(response.getEntity()));
        } finally {
            server.stop(0);
        }
    }
}
