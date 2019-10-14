package org.linuxprobe.luava;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.linuxprobe.luava.http.HttpRequestUtils;

import java.io.IOException;

public class QsTest {
    @Test
    public void run() throws IOException {
        HttpRequestUtils httpRequestUtils = new HttpRequestUtils();
        ByteArrayEntity entity = new ByteArrayEntity(new byte[10000]);
        CloseableHttpResponse reponse = httpRequestUtils.httpRequest("GET", "https://www.baidu.com", null, entity);
        System.out.println(EntityUtils.toString(reponse.getEntity()));
    }
}
