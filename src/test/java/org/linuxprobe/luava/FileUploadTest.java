package org.linuxprobe.luava;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.io.File;
import java.io.IOException;

public class FileUploadTest {

    @Test
    public void requestTest() throws IOException {
        File file = new File("d:/历史数据.xlsx");
        Header header = new BasicHeader("token", "test");
        HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("file", file)
                .addPart("pro_id", new StringBody("11808", ContentType.TEXT_PLAIN))
                .addPart("pro_batch_id", new StringBody("F106871A-C6DA-19E9-E053-0621680AD56F",
                        ContentType.TEXT_PLAIN))
                .addPart("region_id", new StringBody("E40C3C20-0F77-6A48-E053-0621680A518F",
                        ContentType.TEXT_PLAIN))
                .addPart("acceptance_channel", new StringBody("3", ContentType.TEXT_PLAIN)).build();
        CloseableHttpResponse post = new HttpRequestUtils().postRequest("http://127.0.0.1:18080/update", entity, header);
        System.out.println("test");
    }
}
