package org.linuxprobe.luava;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.HttpRequestSpec;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * multipart文件上传测试。
 */
public class FileUploadTest {

    /**
     * 验证临时文件和普通表单字段能够通过multipart请求上传。
     *
     * @throws Exception 文件、请求或服务处理失败
     */
    @Test
    public void shouldUploadMultipartFile() throws Exception {
        byte[] fileContent = "文件上传测试内容".getBytes(StandardCharsets.UTF_8);
        Path uploadFile = Files.createTempFile("luava-http-upload-", ".txt");
        Files.write(uploadFile, fileContent);
        AtomicReference<String> requestContentType = new AtomicReference<>();
        AtomicReference<byte[]> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/upload", exchange -> {
            requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(readRequestBody(exchange.getRequestBody()));
            byte[] responseBody = "success".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
        try {
            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", uploadFile.toFile(), ContentType.APPLICATION_OCTET_STREAM,
                            uploadFile.getFileName().toString())
                    .addTextBody("description", "multipart-test",
                            ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                    .build();
            try (HttpRequestUtils requestUtils = new HttpRequestUtils();
                 CloseableHttpResponse response = requestUtils.execute(
                         HttpRequestSpec.builder(
                                         HttpPost.METHOD_NAME,
                                         "http://127.0.0.1:"
                                                 + server.getAddress()
                                                 .getPort()
                                                 + "/upload")
                                 .entityBody(multipartEntity)
                                 .build())) {
                Assert.assertEquals(200, response.getStatusLine().getStatusCode());
                Assert.assertEquals("success", EntityUtils.toString(response.getEntity()));
            }
            Assert.assertNotNull(requestContentType.get());
            Assert.assertTrue(requestContentType.get().startsWith("multipart/form-data;"));
            Assert.assertNotNull(requestBody.get());
            String multipartBody = new String(requestBody.get(), StandardCharsets.ISO_8859_1);
            Assert.assertTrue(multipartBody.contains("name=\"file\""));
            Assert.assertTrue(multipartBody.contains(
                    "filename=\"" + uploadFile.getFileName().toString() + "\""));
            Assert.assertTrue(multipartBody.contains(new String(fileContent, StandardCharsets.ISO_8859_1)));
            Assert.assertTrue(multipartBody.contains("name=\"description\""));
            Assert.assertTrue(multipartBody.contains("multipart-test"));
        } finally {
            server.stop(0);
            Files.deleteIfExists(uploadFile);
        }
    }

    /**
     * 读取请求体。
     *
     * @param inputStream 请求输入流
     * @return 请求体字节
     * @throws IOException 读取失败
     */
    private static byte[] readRequestBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readLength;
        while ((readLength = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, readLength);
        }
        return outputStream.toByteArray();
    }
}
