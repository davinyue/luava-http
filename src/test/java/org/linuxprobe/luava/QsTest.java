package org.linuxprobe.luava;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.linuxprobe.luava.http.Qs;

public class QsTest {
	@Test
	public void run() {
		String[] ids = { "123", "56" };
		Map<String, Object> param = new HashMap<>();
		param.put("ids", ids);
		System.out.println(Qs.stringify(param));
	}
}
