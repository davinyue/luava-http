package org.linuxprobe.luava;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rdlinux.luava.http.HttpRequestUtils;
import org.rdlinux.luava.http.Qs;

import java.io.IOException;
import java.util.HashMap;

public class QsTest {
    @Test
    public void run() throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", "zhangsan");
        HashMap<String, Object> students = new HashMap<>();
        String[] ss = {"a", "b"};
        map.put("sz", ss);
        students.put("a", "z");
        students.put("b", "y");
        map.put("students", students);
        System.out.println(Qs.stringify(map));
    }

    @Test
    public void requestTest() throws IOException {
        CloseableHttpResponse post = new HttpRequestUtils().getRequest(
                "http://test-gas-station.ybsjyyn.com/login?clientName=tengyun_oauth2");
        System.out.println(EntityUtils.toString(post.getEntity()));
    }
}
