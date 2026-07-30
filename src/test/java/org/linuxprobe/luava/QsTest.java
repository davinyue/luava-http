package org.linuxprobe.luava;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.Qs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 查询参数序列化测试。
 */
public class QsTest {
    /**
     * 验证参数名、集合值和基本类型数组使用 UTF-8 编码。
     */
    @Test
    public void shouldEncodeParametersAndPrimitiveArrays() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("姓名", "张 三");
        parameters.put("ids", new int[]{1, 2});

        Assert.assertEquals(
                "%E5%A7%93%E5%90%8D=%E5%BC%A0+%E4%B8%89&ids=1&ids=2",
                Qs.stringify(parameters));
    }
}
