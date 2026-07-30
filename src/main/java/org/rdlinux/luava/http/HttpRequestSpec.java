package org.rdlinux.luava.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.rdlinux.luava.json.JacksonUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 类型化 HTTP 请求描述。
 *
 * <p>请求描述只保存单次请求数据，不持有连接或响应资源。</p>
 */
public final class HttpRequestSpec {
    /**
     * HTTP 请求方法。
     */
    private final String method;
    /**
     * 请求地址。
     */
    private final String url;
    /**
     * URL 查询参数。
     */
    private final Object queryParams;
    /**
     * 请求正文实体。
     */
    private final HttpEntity bodyEntity;
    /**
     * 单次请求配置。
     */
    private final HttpRequestOptions requestOptions;
    /**
     * 请求头。
     */
    private final Header[] headers;

    /**
     * 根据 Builder 创建请求描述。
     *
     * @param builder 请求描述 Builder
     */
    private HttpRequestSpec(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.queryParams = builder.queryParams;
        this.bodyEntity = builder.bodyEntity;
        this.requestOptions = builder.requestOptions;
        this.headers = builder.headers.toArray(
                new Header[builder.headers.size()]);
    }

    /**
     * 创建请求描述 Builder。
     *
     * @param method HTTP 请求方法
     * @param url 请求地址
     * @return 请求描述 Builder
     */
    public static Builder builder(String method, String url) {
        return new Builder(method, url);
    }

    /**
     * 获取 HTTP 请求方法。
     *
     * @return HTTP 请求方法
     */
    public String getMethod() {
        return this.method;
    }

    /**
     * 获取请求地址。
     *
     * @return 请求地址
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * 获取查询参数。
     *
     * @return 查询参数
     */
    public Object getQueryParams() {
        return this.queryParams;
    }

    /**
     * 获取请求正文实体。
     *
     * @return 请求正文实体，未配置时为 null
     */
    public HttpEntity getBodyEntity() {
        return this.bodyEntity;
    }

    /**
     * 获取单次请求配置。
     *
     * @return 单次请求配置，未配置时为 null
     */
    public HttpRequestOptions getRequestOptions() {
        return this.requestOptions;
    }

    /**
     * 获取请求头副本。
     *
     * @return 请求头副本
     */
    public Header[] getHeaders() {
        return this.headers.clone();
    }

    /**
     * 类型化 HTTP 请求描述 Builder。
     */
    public static final class Builder {
        /**
         * HTTP 请求方法。
         */
        private final String method;
        /**
         * 请求地址。
         */
        private final String url;
        /**
         * URL 查询参数。
         */
        private Object queryParams;
        /**
         * 请求正文实体。
         */
        private HttpEntity bodyEntity;
        /**
         * 是否已经设置正文。
         */
        private boolean bodyConfigured;
        /**
         * 单次请求配置。
         */
        private HttpRequestOptions requestOptions;
        /**
         * 请求头集合。
         */
        private final List<Header> headers = new ArrayList<>();

        /**
         * 创建请求描述 Builder。
         *
         * @param method HTTP 请求方法
         * @param url 请求地址
         */
        private Builder(String method, String url) {
            if (method == null || method.trim().isEmpty()) {
                throw new IllegalArgumentException("method can not be blank");
            }
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("url can not be blank");
            }
            this.method = method.trim();
            this.url = url.trim();
        }

        /**
         * 设置 URL 查询参数。
         *
         * @param queryParams 查询参数对象、Map 或查询字符串
         * @return 当前 Builder
         */
        public Builder query(Object queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        /**
         * 设置 JSON 正文。
         *
         * <p>字符串保持原值，其他对象使用 Jackson 转换为 JSON。</p>
         *
         * @param body JSON 字符串或待序列化对象
         * @return 当前 Builder
         */
        public Builder jsonBody(Object body) {
            if (body == null) {
                throw new IllegalArgumentException("body can not be null");
            }
            String bodyString;
            if (body instanceof String) {
                bodyString = (String) body;
            } else if (body instanceof StringBuilder
                    || body instanceof StringBuffer) {
                bodyString = body.toString();
            } else {
                bodyString = JacksonUtils.toJsonString(body);
            }
            return this.body(new StringEntity(
                    bodyString, ContentType.APPLICATION_JSON));
        }

        /**
         * 设置 application/x-www-form-urlencoded 正文。
         *
         * @param form 表单参数对象或 Map
         * @return 当前 Builder
         */
        public Builder formBody(Object form) {
            if (form == null) {
                throw new IllegalArgumentException("form can not be null");
            }
            return this.body(new StringEntity(
                    Qs.stringify(form),
                    ContentType.create(
                            ContentType.APPLICATION_FORM_URLENCODED
                                    .getMimeType(),
                            StandardCharsets.UTF_8)));
        }

        /**
         * 设置 Apache HTTP 实体正文。
         *
         * @param entity HTTP 实体
         * @return 当前 Builder
         */
        public Builder entityBody(HttpEntity entity) {
            return this.body(entity);
        }

        /**
         * 设置指定内容类型的文本正文。
         *
         * @param text 文本正文
         * @param contentType 内容类型
         * @return 当前 Builder
         */
        public Builder textBody(String text, ContentType contentType) {
            if (text == null) {
                throw new IllegalArgumentException("text can not be null");
            }
            if (contentType == null) {
                throw new IllegalArgumentException(
                        "contentType can not be null");
            }
            return this.body(new StringEntity(text, contentType));
        }

        /**
         * 设置单次请求配置。
         *
         * @param requestOptions 单次请求配置
         * @return 当前 Builder
         */
        public Builder options(HttpRequestOptions requestOptions) {
            if (requestOptions == null) {
                throw new IllegalArgumentException(
                        "requestOptions can not be null");
            }
            this.requestOptions = requestOptions;
            return this;
        }

        /**
         * 添加请求头。
         *
         * @param name 请求头名称
         * @param value 请求头值
         * @return 当前 Builder
         */
        public Builder header(String name, String value) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "header name can not be blank");
            }
            if (value == null) {
                throw new IllegalArgumentException(
                        "header value can not be null");
            }
            return this.header(new BasicHeader(name, value));
        }

        /**
         * 添加请求头。
         *
         * @param header 请求头
         * @return 当前 Builder
         */
        public Builder header(Header header) {
            if (header == null) {
                throw new IllegalArgumentException("header can not be null");
            }
            this.headers.add(header);
            return this;
        }

        /**
         * 批量添加请求头。
         *
         * @param headers 请求头
         * @return 当前 Builder
         */
        public Builder headers(Header... headers) {
            if (headers == null) {
                return this;
            }
            for (Header header : headers) {
                if (header != null) {
                    this.headers.add(header);
                }
            }
            return this;
        }

        /**
         * 创建类型化 HTTP 请求描述。
         *
         * @return 类型化 HTTP 请求描述
         */
        public HttpRequestSpec build() {
            return new HttpRequestSpec(this);
        }

        /**
         * 设置互斥请求正文。
         *
         * @param bodyEntity 请求正文实体
         * @return 当前 Builder
         */
        private Builder body(HttpEntity bodyEntity) {
            if (bodyEntity == null) {
                throw new IllegalArgumentException(
                        "bodyEntity can not be null");
            }
            if (this.bodyConfigured) {
                throw new IllegalStateException(
                        "request body already configured");
            }
            this.bodyConfigured = true;
            this.bodyEntity = bodyEntity;
            return this;
        }
    }
}
