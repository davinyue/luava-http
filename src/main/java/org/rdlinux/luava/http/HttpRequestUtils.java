package org.rdlinux.luava.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.json.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * 基于Apache HttpClient的HTTP请求工具。
 */
public class HttpRequestUtils implements AutoCloseable {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(HttpRequestUtils.class);
    /**
     * 连接池配置
     */
    private final ConnectPool connectPool;
    /**
     * 是否显式信任全部TLS证书并关闭主机名校验。
     */
    private final boolean insecureTrustAllCertificates;
    /**
     * 当前工具持有的HTTP客户端
     */
    private volatile CloseableHttpClient httpClient;
    /**
     * 工具关闭状态
     */
    private volatile boolean closed;

    /**
     * 构造一个使用连接池的http请求工具
     *
     * @param connectPool 连接池配置对象,当为null时,使用默认配置
     */
    public HttpRequestUtils(ConnectPool connectPool) {
        this(connectPool, false);
    }

    /**
     * 构造一个使用连接池的HTTP请求工具。
     *
     * @param connectPool                  连接池配置对象，为null时使用默认配置
     * @param insecureTrustAllCertificates 是否信任全部TLS证书并关闭主机名校验
     */
    public HttpRequestUtils(ConnectPool connectPool,
                            boolean insecureTrustAllCertificates) {
        if (connectPool == null) {
            connectPool = new ConnectPool();
        }
        this.connectPool = connectPool.copy();
        this.insecureTrustAllCertificates = insecureTrustAllCertificates;
    }

    /**
     * 使用默认连接池配置构造HTTP请求工具。
     */
    public HttpRequestUtils() {
        this(null);
    }

    /**
     * 消费响应正文并关闭响应。
     *
     * <p>响应没有正文时返回空字符串。</p>
     *
     * @param response HTTP 响应
     * @return 响应正文
     */
    public static String consumeResponseAsString(
            CloseableHttpResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response can not be null");
        }
        try {
            HttpEntity entity = response.getEntity();
            return entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (ParseException | IOException e1) {
            throw new HttpResponseConsumeException(e1);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                log.error("HTTP响应关闭失败", e);
            }
        }
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, Class<T> type) {
        return JacksonUtils.conversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, Class<T> type) {
        return JacksonUtils.snakeCaseConversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, Type type) {
        return JacksonUtils.conversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, Type type) {
        return JacksonUtils.snakeCaseConversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, JavaType type) {
        return JacksonUtils.conversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, JavaType type) {
        return JacksonUtils.snakeCaseConversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, TypeReference<T> type) {
        return JacksonUtils.conversion(consumeResponseAsString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, TypeReference<T> type) {
        return JacksonUtils.snakeCaseConversion(consumeResponseAsString(response), type);
    }

    /**
     * 创建包含查询参数的请求 URI。
     *
     * @param url       原始请求地址
     * @param urlParams URL 查询参数
     * @return 请求 URI
     */
    private static URI createRequestUri(String url, Object urlParams) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url, StandardCharsets.UTF_8);
            if (urlParams == null) {
                return uriBuilder.build();
            }
            String queryString = Qs.stringify(urlParams);
            if (!queryString.isEmpty()) {
                uriBuilder.addParameters(URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8));
            }
            return uriBuilder.build();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("invalid request url: " + url, exception);
        }
    }

    /**
     * 创建请求正文跟踪实体。
     *
     * @param bodyEntity 请求正文实体
     * @return 请求正文跟踪实体，无正文时为 null
     */
    private static RequestWriteTrackingEntity createTrackingEntity(HttpEntity bodyEntity) {
        if (bodyEntity == null) {
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("本次请求body类型:{}, 长度:{}",
                    bodyEntity.getContentType(),
                    bodyEntity.getContentLength());
        }
        return new RequestWriteTrackingEntity(bodyEntity);
    }

    /**
     * 根据是否存在正文创建 Apache 请求对象。
     *
     * @param method         HTTP 请求方法
     * @param trackingEntity 请求正文跟踪实体
     * @return Apache 请求对象
     */
    private static HttpRequestBase createRequest(
            String method, RequestWriteTrackingEntity trackingEntity) {
        if (trackingEntity == null) {
            return new HttpRequestBase() {
                @Override
                public String getMethod() {
                    return method;
                }
            };
        }
        HttpEntityEnclosingRequestBase request =
                new HttpEntityEnclosingRequestBase() {
                    @Override
                    public String getMethod() {
                        return method;
                    }
                };
        request.setEntity(trackingEntity);
        return request;
    }

    /**
     * 请求头不存在时从 MDC 注入请求头。
     *
     * @param request    Apache 请求对象
     * @param headerName 请求头名称
     * @param mdcName    MDC 属性名称
     */
    private static void addMdcHeaderIfAbsent(
            HttpRequestBase request, String headerName, String mdcName) {
        if (request.getFirstHeader(headerName) != null) {
            return;
        }
        String value = MDC.get(mdcName);
        if (value != null && !value.isEmpty()) {
            request.setHeader(headerName, value);
        }
    }

    /**
     * 执行类型化 HTTP 请求。
     *
     * @param requestSpec HTTP 请求描述
     * @return HTTP 响应，调用方必须关闭该响应
     */
    public CloseableHttpResponse execute(HttpRequestSpec requestSpec) {
        if (requestSpec == null) {
            throw new IllegalArgumentException("requestSpec can not be null");
        }
        return this.executeHttpRequest(requestSpec);
    }

    /**
     * 执行类型化 HTTP 请求并处理响应。
     *
     * <p>该方法在响应处理完成或发生异常后自动关闭响应。</p>
     *
     * @param requestSpec     HTTP 请求描述
     * @param responseHandler HTTP 响应处理器
     * @param <T>             响应处理结果类型
     * @return 响应处理结果
     */
    public <T> T execute(HttpRequestSpec requestSpec, HttpResponseHandler<T> responseHandler) {
        if (responseHandler == null) {
            throw new IllegalArgumentException(
                    "responseHandler can not be null");
        }
        try (CloseableHttpResponse response = this.execute(requestSpec)) {
            return responseHandler.handle(response);
        } catch (IOException exception) {
            throw new HttpResponseConsumeException(exception);
        }
    }

    /**
     * 执行HTTP请求。
     *
     * @param requestSpec HTTP 请求描述
     * @return HTTP响应，调用方必须关闭该响应
     */
    private CloseableHttpResponse executeHttpRequest(HttpRequestSpec requestSpec) {
        URI requestUri = createRequestUri(
                requestSpec.getUrl(), requestSpec.getQueryParams());
        if (log.isTraceEnabled()) {
            log.trace("本次请求地址:{}", requestUri);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        RequestWriteTrackingEntity trackingEntity = createTrackingEntity(requestSpec.getBodyEntity());
        HttpRequestBase request = createRequest(requestSpec.getMethod(), trackingEntity);
        request.setURI(requestUri);
        HttpRequestOptions requestOptions = requestSpec.getRequestOptions();
        if (requestOptions != null) {
            request.setConfig(this.createRequestConfig(requestOptions));
        }
        Header[] headers = requestSpec.getHeaders();
        if (headers != null) {
            for (Header header : headers) {
                if (header != null) {
                    request.addHeader(header);
                }
            }
        }
        addMdcHeaderIfAbsent(request, SleuthConst.traceIdHeader, SleuthConst.traceIdLogName);
        addMdcHeaderIfAbsent(request, SleuthConst.spanIdHeader, SleuthConst.spanIdLogName);
        addMdcHeaderIfAbsent(request, SleuthConst.parentSpanIdHeader, SleuthConst.parentSpanIdLogName);
        try {
            return httpclient.execute(request);
        } catch (IOException e) {
            RequestWriteState writeState = trackingEntity == null
                    ? RequestWriteState.NOT_STARTED
                    : trackingEntity.getWriteState();
            throw new HttpRequestExecuteException(e, writeState);
        }
    }

    /**
     * 创建单次Apache请求配置。
     *
     * @param requestOptions 单次请求配置
     * @return Apache请求配置
     */
    private RequestConfig createRequestConfig(
            HttpRequestOptions requestOptions) {
        return RequestConfig.custom()
                .setConnectTimeout(requestOptions.hasConnectTimeout()
                        ? requestOptions.getConnectTimeout()
                        : this.connectPool.getConnectTimeout())
                .setSocketTimeout(requestOptions.hasSocketTimeout()
                        ? requestOptions.getSocketTimeout()
                        : this.connectPool.getSocketTimeout())
                .setConnectionRequestTimeout(
                        requestOptions.hasConnectionRequestTimeout()
                                ? requestOptions.getConnectionRequestTimeout()
                                : this.connectPool
                                .getConnectionRequestTimeout())
                .setRedirectsEnabled(requestOptions.hasRedirectsEnabled()
                        ? requestOptions.isRedirectsEnabled()
                        : RequestConfig.DEFAULT.isRedirectsEnabled())
                .build();
    }

    /**
     * 获取当前工具复用的HTTP客户端。
     *
     * @return HTTP客户端
     */
    public CloseableHttpClient getHttpClient() {
        if (this.closed) {
            throw new IllegalStateException("http request utils already closed");
        }
        if (this.httpClient == null) {
            synchronized (this) {
                if (this.closed) {
                    throw new IllegalStateException("http request utils already closed");
                }
                if (this.httpClient == null) {
                    this.httpClient = new CloseableHttpClientBuilder()
                            .setConnectPool(this.connectPool)
                            .setInsecureTrustAllCertificates(
                                    this.insecureTrustAllCertificates)
                            .build();
                }
            }
        }
        return this.httpClient;
    }

    /**
     * 关闭连接池
     */
    @Override
    public void close() {
        CloseableHttpClient client;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            client = this.httpClient;
            this.httpClient = null;
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            log.error("关闭HTTP客户端失败", e);
        }
    }

    /**
     * 跟踪请求正文的客户端写出阶段。
     */
    private static final class RequestWriteTrackingEntity
            extends HttpEntityWrapper {
        /**
         * 当前写出状态。
         */
        private volatile RequestWriteState writeState =
                RequestWriteState.NOT_STARTED;

        /**
         * 创建请求正文跟踪包装器。
         *
         * @param wrappedEntity 原请求正文
         */
        private RequestWriteTrackingEntity(HttpEntity wrappedEntity) {
            super(wrappedEntity);
        }

        /**
         * 写出请求正文并更新状态。
         *
         * @param outputStream 客户端输出流
         * @throws IOException 请求正文写出失败
         */
        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
            this.writeState = RequestWriteState.STARTED;
            super.writeTo(outputStream);
            this.writeState = RequestWriteState.COMPLETED;
        }

        /**
         * 获取当前写出状态。
         *
         * @return 当前写出状态
         */
        private RequestWriteState getWriteState() {
            return this.writeState;
        }
    }
}
