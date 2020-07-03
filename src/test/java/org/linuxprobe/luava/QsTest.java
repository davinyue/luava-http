package org.linuxprobe.luava;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.linuxprobe.luava.http.HttpRequestUtils;
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

    @Test
    public void requestTest() throws IOException {
        CloseableHttpResponse post = new HttpRequestUtils().httpRequest("POST", "https://sandbox.api.openmidas.com/v1/r/1450013764/unified_order?amount=1000&buss_settle_amount=0&callback_url=https://d-mid.ybsjyyn.com/trade/payment/notify/result/cmb/pay&channel=wechat&currency_type=CNY&need_settle_check=1&num=1&original_amount=1000&out_trade_no=42004030000022&platform_income=1000&product_detail=测试商品2&product_id=1001&product_name=萝卜&real_channel=OPENBANK_CMB&settle_appid=cmb2101012005&sub_appid=cmb2101012005&sub_out_trade_no_list=[{\"sub_out_trade_no\":\"4200403000002201\",\"amount\":1000,\"product_name\":\"萝卜\",\"product_detail\":\"测试商品2\",\"platform_income\":1000,\"buss_settle_amount\":0,\"original_amount\":1000,\"sub_appid\":\"cmb\"}]&ts=1589217578&user_id=5df73cf9f597ca02ab7576c3&sign=PWe1rbdIiUscan9P6eULD4h%2FKzn70jFJnp5kW5Wt%2FBM0q%2Be5f36somwtxiUva9RIJOYfp3Sc25KNrXRwosSDli3WlWScKfMEj5fVO9oTqgTC09u3AG2I7oyjm7r2fZZQQ9KObaD7YfCvRZOKb2lCZKZxFVt3S2ZUaxZNczSVyaDEjA9HYQDhGRJUgYjEo7Vijg5JVFmrz0DojWrtVwQGbThCntr8qea%2BNrXdSY%2BymOKAtAbxb8CxtsHIW%2BRP0ptUK2irE0Oo%2BAFAAI%2FPONqMndpf046ka2F2LJVhJzH%2B8A%2FxchRBLVadl9zgjv5i%2FXjy9DHDwdAXpLeFnuokpe2Aog%3D%3D",
                null, null, true);
        System.out.println(EntityUtils.toString(post.getEntity()));
    }
}
