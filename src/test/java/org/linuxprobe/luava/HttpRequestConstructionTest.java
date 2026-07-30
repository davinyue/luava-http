package org.linuxprobe.luava;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.luava.http.HttpRequestUtils;

import java.lang.reflect.Method;

/**
 * HTTP 请求对象构造测试。
 */
public class HttpRequestConstructionTest {
    /**
     * 验证仅在存在正文时构造可携带正文的请求对象。
     *
     * @throws Exception 反射调用失败
     */
    @Test
    public void shouldOnlyCreateEntityRequestWhenBodyExists()
            throws Exception {
        Method createTrackingEntity = HttpRequestUtils.class
                .getDeclaredMethod(
                        "createTrackingEntity", HttpEntity.class);
        createTrackingEntity.setAccessible(true);
        Object trackingEntity = createTrackingEntity.invoke(
                null, new StringEntity("body", ContentType.TEXT_PLAIN));

        Method createRequest = HttpRequestUtils.class.getDeclaredMethod(
                "createRequest",
                String.class,
                trackingEntity.getClass());
        createRequest.setAccessible(true);
        Object bodylessRequest = createRequest.invoke(
                null, "GET", null);
        Object bodyRequest = createRequest.invoke(
                null, HttpPost.METHOD_NAME, trackingEntity);

        Assert.assertFalse(
                bodylessRequest instanceof HttpEntityEnclosingRequest);
        Assert.assertTrue(bodyRequest instanceof HttpEntityEnclosingRequest);
        Assert.assertNotNull(
                ((HttpEntityEnclosingRequest) bodyRequest).getEntity());
        Assert.assertTrue(
                ((HttpEntityEnclosingRequest) bodyRequest).getEntity()
                        instanceof HttpEntity);
    }
}
