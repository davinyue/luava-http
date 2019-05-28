package org.linuxprobe.luava.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linuxprobe.luava.json.JacksonUtils;

/** http请求参数工具 */
public class Qs {

	public static StringBuilder stringify(String prefix, Map<?, ?> mapParam) {
		StringBuilder resultBuilder = new StringBuilder();
		Set<?> keys = mapParam.keySet();
		for (Object key : keys) {
			Object value = mapParam.get(key);
			if (value instanceof List) {
				List<?> sonParams = (List<?>) value;
				for (Object sonParam : sonParams) {
					if (sonParam instanceof Map) {
						resultBuilder.append(stringify(prefix + key.toString() + ".", (Map<?, ?>) sonParam));
					} else {
						resultBuilder.append(prefix + key.toString() + "=" + sonParam);
					}
				}
			} else if (value instanceof Map) {
				resultBuilder.append(stringify(prefix + key.toString() + ".", (Map<?, ?>) value));
			} else {
				String strvalue = null;
				try {
					strvalue = URLEncoder.encode(value.toString(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException(e);
				}
				resultBuilder.append(prefix + key.toString() + "=" + strvalue + "&");
			}
		}
		return resultBuilder;
	}

	/** 把对象转换为http请求参数,注意不支持传入容器和数组,传入string时将直接返回 */
	public static String stringify(Object object) {
		if (object instanceof Iterable) {
			throw new IllegalArgumentException("不支持容器数据");
		} else if (object.getClass().isArray()) {
			throw new IllegalArgumentException("不支持数组数据");
		} else if (object instanceof Number) {
			throw new IllegalArgumentException("不支持数字数据");
		} else if (object.getClass().equals(java.lang.Integer.class) || object.getClass().equals(java.lang.Byte.class)
				|| object.getClass().equals(java.lang.Long.class) || object.getClass().equals(java.lang.Double.class)
				|| object.getClass().equals(java.lang.Float.class)
				|| object.getClass().equals(java.lang.Character.class)
				|| object.getClass().equals(java.lang.Short.class)
				|| object.getClass().equals(java.lang.Boolean.class)) {
			throw new IllegalArgumentException("不支持基本类型数据");
		} else {
			if (object instanceof String) {
				return (String) object;
			} else {
				Map<?, ?> mapParam = null;
				if (object instanceof Map) {
					mapParam = (Map<?, ?>) object;
				} else {
					mapParam = JacksonUtils.conversion(object, Map.class);
				}
				StringBuilder resultBuilder = stringify("", mapParam);
				if (resultBuilder.length() > 0) {
					resultBuilder.delete(resultBuilder.length() - 1, resultBuilder.length());
				}
				return resultBuilder.toString();
			}
		}
	}
}