package org.rdlinux.luava.http;


import org.rdlinux.luava.json.JacksonUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * http请求参数工具
 */
public class Qs {
    /**
     * 递归序列化 Map 查询参数。
     *
     * @param prefix 参数名前缀
     * @param mapParam Map 查询参数
     * @return 查询字符串 Builder
     */
    private static StringBuilder stringify(String prefix, Map<?, ?> mapParam) {
        StringBuilder resultBuilder = new StringBuilder();
        Set<?> keys = mapParam.keySet();
        for (Object key : keys) {
            Object value = mapParam.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Collection || value.getClass().isArray()) {
                if (value instanceof Collection) {
                    for (Object sonParam : (Collection<?>) value) {
                        appendValue(resultBuilder, prefix, key, sonParam);
                    }
                } else {
                    int length = Array.getLength(value);
                    for (int index = 0; index < length; index++) {
                        appendValue(resultBuilder, prefix, key,
                                Array.get(value, index));
                    }
                }
            } else if (value instanceof Map) {
                resultBuilder.append(stringify(prefix + key.toString() + ".", (Map<?, ?>) value));
            } else {
                appendParameter(resultBuilder,
                        prefix + key.toString(), value);
            }
        }
        return resultBuilder;
    }

    /**
     * 追加集合或数组中的参数值。
     *
     * @param resultBuilder 查询字符串 Builder
     * @param prefix 参数名前缀
     * @param key 参数名
     * @param value 参数值
     */
    private static void appendValue(StringBuilder resultBuilder,
                                    String prefix, Object key, Object value) {
        if (value instanceof Map) {
            resultBuilder.append(stringify(
                    prefix + key.toString() + ".", (Map<?, ?>) value));
        } else if (value != null) {
            appendParameter(resultBuilder,
                    prefix + key.toString(), value);
        }
    }

    /**
     * 追加经过 URL 编码的查询参数。
     *
     * @param resultBuilder 查询字符串 Builder
     * @param name 参数名
     * @param value 参数值
     */
    private static void appendParameter(StringBuilder resultBuilder,
                                        String name, Object value) {
        try {
            resultBuilder.append(URLEncoder.encode(
                            name, StandardCharsets.UTF_8.name()))
                    .append("=")
                    .append(URLEncoder.encode(
                            value.toString(),
                            StandardCharsets.UTF_8.name()))
                    .append("&");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    /**
     * 把对象转换为http请求参数,注意不支持传入容器和数组,传入string时将直接返回
     *
     * @param object 查询参数对象
     * @return URL 编码的查询字符串
     */
    public static String stringify(Object object) {
        if (object instanceof Iterable) {
            throw new IllegalArgumentException("不支持容器数据");
        } else if (object.getClass().isArray()) {
            throw new IllegalArgumentException("不支持数组数据");
        } else if (object instanceof Number) {
            throw new IllegalArgumentException("不支持数字数据");
        } else if (object.getClass().equals(int.class) || object.getClass().equals(byte.class)
                || object.getClass().equals(long.class) || object.getClass().equals(double.class)
                || object.getClass().equals(float.class)
                || object.getClass().equals(char.class)
                || object.getClass().equals(short.class)
                || object.getClass().equals(boolean.class)) {
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
