package org.linuxprobe.luava;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.linuxprobe.luava.http.ConnectPool;
import org.linuxprobe.luava.http.HttpRequestUtils;
import org.linuxprobe.luava.http.Qs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class QsTest {
    @Test
    public void run() {
        String[] ids = {"123", "56"};
        Map<String, Object> param = new HashMap<>();
        param.put("ids", ids);
        System.out.println(Qs.stringify(param));
    }

    @Test
    public void testRequest() throws InterruptedException {
        MyThread myThread = new MyThread();
        int threadNum = 200;
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(myThread);
            thread.setName("request-" + (i + 1));
            threads[i] = thread;
            thread.start();
        }
        for (int i = 0; i < threadNum; i++) {
            threads[i].join();
        }
    }

    public static class MyThread implements Runnable {
        public HttpRequestUtils httpRequestUtils;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public MyThread() {
            ConnectPool connectPool = new ConnectPool();
            connectPool.setMaxIdleTime(30000);
            connectPool.setSingleMaxActive(30);
            connectPool.setCleanSleepTime(10000);
            this.httpRequestUtils = new HttpRequestUtils(connectPool);
        }

        @Override
        public void run() {
            while (true) {
                String param = "{\n" +
                        "    \"app_id\":\"1b256b568967e072\",\n" +
                        "    \"access_token\":\"RlhaV3gxYks1VVFXMjB3QmdmLzVaTE1jNGZKc25jUmRlLzFvTks3bVVVOXo0QU8rZnZNa1ZKNERmdUxXelhoZHl1N0FsZzZJd1VHYTAvTDZUWVZpay9BUzdIN3BTZWZkN3RjcFdSS3E0OEU9\",\n" +
                        "    \"cmq_str\":\"ota_order_notice\",\n" +
                        "    \"other\":{\n" +
                        "        \"order_no\":\"801011685649230538\",\n" +
                        "        \"partner_sub_orderno\":\"\",\n" +
                        "        \"operate_type\":1,\n" +
                        "        \"sub_order_status\":4,\n" +
                        "        \"sub_order_no\":\"802036140549159261\",\n" +
                        "        \"num\":1,\n" +
                        "        \"partner_order_no\":\"1563951549891\",\n" +
                        "        \"order_type\":1,\n" +
                        "        \"order_state\":4,\n" +
                        "        \"operate_time\":\"2019-07-24 14:59:47\"\n" +
                        "    }\n" +
                        "}";
                BasicHeader appId = new BasicHeader("appId", "1b256b568967e072");
                BasicHeader token = new BasicHeader("accessToken", "RlhaV3gxYks1VVFXMjB3QmdmLzVaTE1jNGZKc25jUmRlLzFvTks3bVVVOXo0QU8rZnZNa1ZKNERmdUxXelhoZHl1N0FsZzZJd1VHYTAvTDZUWVZpay9BUzdIN3BTZWZkN3RjcFdSS3E0OEU9");
                long start = System.currentTimeMillis();
                HttpResponse httpResponse = this.httpRequestUtils.postRequest("https://ticket.ybsjyyn.com/mtsale/fsale/orderChange/notice", (Object) param, appId, token);
                //HttpResponse httpResponse = this.httpRequestUtils.postRequest("http://127.0.0.1:5000/mtsale/fsale/orderChange/notice", (Object) param, appId, token);
                //HttpRequestUtils.responseDataConversion(httpResponse, Map.class);
                //System.out.println(HttpRequestUtils.responseDataConversion(httpResponse, Map.class));
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                long end = System.currentTimeMillis();
                long sub = end - start;
                if (sub > 1000) {
                    System.err.println(this.simpleDateFormat.format(new Date()) + ", " + Thread.currentThread().getName() + "耗时" + sub + ", 超时");
                } else {
                    System.out.println(this.simpleDateFormat.format(new Date()) + ", " + Thread.currentThread().getName() + "耗时" + sub);
                }
                try {
                    Thread.sleep(5 * 60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
