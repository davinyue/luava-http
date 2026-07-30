package org.rdlinux.luava.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.json.JacksonUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 常用 HTTP 响应处理器。
 */
public final class HttpResponseHandlers {
    /**
     * 禁止实例化工具类。
     */
    private HttpResponseHandlers() {
    }

    /**
     * 创建字符串响应处理器。
     *
     * @return 字符串响应处理器
     */
    public static HttpResponseHandler<String> asString() {
        return response -> {
            HttpEntity entity = response.getEntity();
            return entity == null
                    ? ""
                    : EntityUtils.toString(entity, StandardCharsets.UTF_8);
        };
    }

    /**
     * 创建普通 JSON 对象响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asJson(Class<T> type) {
        return response -> JacksonUtils.conversion(
                asString().handle(response), type);
    }

    /**
     * 创建反射类型 JSON 响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asJson(Type type) {
        return response -> JacksonUtils.conversion(
                asString().handle(response), type);
    }

    /**
     * 创建 Jackson 类型 JSON 响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asJson(JavaType type) {
        return response -> JacksonUtils.conversion(
                asString().handle(response), type);
    }

    /**
     * 创建泛型 JSON 响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asJson(
            TypeReference<T> type) {
        return response -> JacksonUtils.conversion(
                asString().handle(response), type);
    }

    /**
     * 创建 snake_case 普通 JSON 对象响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return snake_case JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asSnakeCaseJson(Class<T> type) {
        return response -> JacksonUtils.snakeCaseConversion(
                asString().handle(response), type);
    }

    /**
     * 创建 snake_case 反射类型 JSON 响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return snake_case JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asSnakeCaseJson(Type type) {
        return response -> JacksonUtils.snakeCaseConversion(
                asString().handle(response), type);
    }

    /**
     * 创建 snake_case Jackson 类型 JSON 响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return snake_case JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asSnakeCaseJson(JavaType type) {
        return response -> JacksonUtils.snakeCaseConversion(
                asString().handle(response), type);
    }

    /**
     * 创建 snake_case 泛型 JSON 响应处理器。
     *
     * @param type 目标类型
     * @param <T> 目标类型
     * @return snake_case JSON 响应处理器
     */
    public static <T> HttpResponseHandler<T> asSnakeCaseJson(
            TypeReference<T> type) {
        return response -> JacksonUtils.snakeCaseConversion(
                asString().handle(response), type);
    }
}
