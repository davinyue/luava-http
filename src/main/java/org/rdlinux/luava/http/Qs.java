package org.rdlinux.luava.http;


import org.rdlinux.luava.json.JacksonUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * http请求参数工具
 */
public class Qs {
    private static StringBuilder stringify(String prefix, Map<?, ?> mapParam) {
        StringBuilder resultBuilder = new StringBuilder();
        Set<?> keys = mapParam.keySet();
        for (Object key : keys) {
            Object value = mapParam.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Collection || value.getClass().isArray()) {
                Collection<?> sonParams = null;
                if (value instanceof Collection) {
                    sonParams = (Collection<?>) value;
                } else {
                    Object[] array = (Object[]) value;
                    sonParams = Arrays.asList(array);
                }
                for (Object sonParam : sonParams) {
                    if (sonParam instanceof Map) {
                        resultBuilder.append(stringify(prefix + key.toString() + ".", (Map<?, ?>) sonParam));
                    } else {
                        resultBuilder.append(prefix).append(key.toString()).append("=").append(sonParam).append("&");
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
                resultBuilder.append(prefix).append(key.toString()).append("=").append(strvalue).append("&");
            }
        }
        return resultBuilder;
    }

    /**
     * 把对象转换为http请求参数,注意不支持传入容器和数组,传入string时将直接返回
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