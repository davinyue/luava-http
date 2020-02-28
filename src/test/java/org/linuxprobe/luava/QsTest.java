package org.linuxprobe.luava;

import org.junit.Test;
import org.linuxprobe.luava.http.Qs;

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
}
