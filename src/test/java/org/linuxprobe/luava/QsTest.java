package org.linuxprobe.luava;

import java.util.Map;

import org.junit.Test;
import org.linuxprobe.luava.http.Qs;
import org.linuxprobe.luava.json.JacksonUtils;

public class QsTest {
	@Test
	public void run() {
		try {
			String json = "{\"name\":\"BeJson\",\"url\":\"http://www.bejson.com\",\"page\":88,\"isNonProfit\":true,\"address\":{\"street\":\"科技园路.\",\"city\":\"江苏苏州\",\"country\":\"中国\"},\"links\":[{\"name\":\"Google\",\"url\":\"http://www.google.com\"},{\"name\":\"Baidu\",\"url\":\"http://www.baidu.com\"},{\"name\":\"SoSo\",\"url\":\"http://www.SoSo.com\"}]}";
			@SuppressWarnings("unchecked")
			Map<String, Object> first = JacksonUtils.conversion(json, Map.class);
			System.out.println(Qs.stringify(first));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
