package org.rdlinux.luava.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
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


/**
 * 基于Apache HttpClient的HTTP请求工具。
 */
public class HttpRequestUtils implements AutoCloseable {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(HttpRequestUtils.class);
    /**
     * 当前工具持有的HTTP客户端
     */
    private volatile CloseableHttpClient httpClient;
    /**
     * 连接池配置
     */
    private final ConnectPool connectPool;
    /**
     * 是否显式信任全部TLS证书并关闭主机名校验。
     */
    private final boolean insecureTrustAllCertificates;
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
     * @param connectPool 连接池配置对象，为null时使用默认配置
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

    private static String responseToString(CloseableHttpResponse response) {
        HttpEntity entity = response.getEntity();
        String entityString;
        try {
            entityString = EntityUtils.toString(entity);
        } catch (ParseException | IOException e1) {
            throw new RuntimeException(e1);
        } finally {
            try {
                EntityUtils.consume(entity);
                response.close();
            } catch (IOException e) {
                log.error("httpClient释放连接失败", e);
            }
        }
        return entityString;
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, Class<T> type) {
        return JacksonUtils.conversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, Class<T> type) {
        return JacksonUtils.snakeCaseConversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, Type type) {
        return JacksonUtils.conversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, Type type) {
        return JacksonUtils.snakeCaseConversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, JavaType type) {
        return JacksonUtils.conversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, JavaType type) {
        return JacksonUtils.snakeCaseConversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(CloseableHttpResponse response, TypeReference<T> type) {
        return JacksonUtils.conversion(responseToString(response), type);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataSnakeConversion(CloseableHttpResponse response, TypeReference<T> type) {
        return JacksonUtils.snakeCaseConversion(responseToString(response), type);
    }

    /**
     * http请求
     *
     * @param method      请求方法
     * @param url         请求地址
     * @param urlParams   url参数
     * @param bodyParam   body参数,如果不是HttpEntity类型,将根据useFormData使用json或formData传递body参数
     * @param useFormData 使用formData传递参数
     * @param headers     请求头
     * @return HTTP响应，调用方必须关闭该响应
     */
    public CloseableHttpResponse httpRequest(String method, String url, Object urlParams, Object bodyParam,
                                             boolean useFormData, Header... headers) {
        return this.executeHttpRequest(method, url, urlParams, bodyParam,
                useFormData, null, headers);
    }

    /**
     * 使用单次请求配置执行HTTP请求。
     *
     * <p>该方法使用独立名称，避免与历史可变参数API在传入null时产生重载歧义。</p>
     *
     * @param method 请求方法
     * @param url 请求地址
     * @param urlParams URL参数
     * @param bodyParam 请求正文；非HttpEntity对象根据useFormData转换
     * @param useFormData 是否使用application/x-www-form-urlencoded
     * @param requestOptions 单次请求配置
     * @param headers 请求头
     * @return HTTP响应，调用方必须关闭该响应
     */
    public CloseableHttpResponse httpRequestWithOptions(
            String method, String url, Object urlParams, Object bodyParam,
            boolean useFormData, HttpRequestOptions requestOptions,
            Header... headers) {
        if (requestOptions == null) {
            throw new IllegalArgumentException("requestOptions can not be null");
        }
        return this.executeHttpRequest(method, url, urlParams, bodyParam,
                useFormData, requestOptions, headers);
    }

    /**
     * 执行HTTP请求。
     *
     * @param method 请求方法
     * @param url 请求地址
     * @param urlParams URL参数
     * @param bodyParam 请求正文
     * @param useFormData 是否使用表单正文
     * @param requestOptions 可选单次请求配置
     * @param headers 请求头
     * @return HTTP响应，调用方必须关闭该响应
     */
    private CloseableHttpResponse executeHttpRequest(
            String method, String url, Object urlParams, Object bodyParam,
            boolean useFormData, HttpRequestOptions requestOptions,
            Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.contains("?")) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return method;
            }
        };
        request.setURI(URI.create(url));
        if (requestOptions != null) {
            request.setConfig(createRequestConfig(requestOptions));
        }
        if (headers != null && headers.length != 0) {
            for (Header header : headers) {
                if (header != null) {
                    request.addHeader(header);
                }
            }
        }
        //添加调用链追踪请求头
        if (request.getFirstHeader(SleuthConst.traceIdHeader) != null) {
            String traceId = MDC.get(SleuthConst.traceIdLogName);
            if (traceId != null) {
                request.addHeader(SleuthConst.traceIdHeader, traceId);
            }
        }
        if (request.getFirstHeader(SleuthConst.spanIdHeader) != null) {
            String spanId = MDC.get(SleuthConst.spanIdLogName);
            if (spanId != null) {
                request.addHeader(SleuthConst.spanIdHeader, spanId);
            }
        }
        if (request.getFirstHeader(SleuthConst.parentSpanIdHeader) != null) {
            String parentSpanId = MDC.get(SleuthConst.parentSpanIdLogName);
            if (parentSpanId != null) {
                request.addHeader(SleuthConst.parentSpanIdHeader, parentSpanId);
            }
        }
        RequestWriteTrackingEntity trackingEntity = null;
        // 处理body参数
        if (bodyParam != null) {
            HttpEntity bodyEntity;
            if (bodyParam instanceof HttpEntity) {
                if (log.isTraceEnabled()) {
                    log.trace("本次请求body参数:" + bodyParam);
                }
                bodyEntity = (HttpEntity) bodyParam;
            } else {
                if (useFormData) {
                    String stringify = Qs.stringify(bodyParam);
                    if (log.isTraceEnabled()) {
                        log.trace("本次请求body参数:" + stringify);
                    }
                    StringEntity formEntity = new StringEntity(
                            stringify, "UTF-8");
                    formEntity.setContentType(
                            "application/x-www-form-urlencoded");
                    formEntity.setContentEncoding("UTF-8");
                    bodyEntity = formEntity;
                } else {
                    String bodyStr;
                    if (bodyParam instanceof String) {
                        bodyStr = (String) bodyParam;
                    } else if (bodyParam instanceof StringBuilder || bodyParam instanceof StringBuffer) {
                        bodyStr = bodyParam.toString();
                    } else {
                        bodyStr = JacksonUtils.toJsonString(bodyParam);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("本次请求body参数:" + bodyStr);
                    }
                    bodyEntity = new StringEntity(bodyStr, ContentType.APPLICATION_JSON);
                }
            }
            trackingEntity = new RequestWriteTrackingEntity(bodyEntity);
            request.setEntity(trackingEntity);
        }
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
    private static RequestConfig createRequestConfig(
            HttpRequestOptions requestOptions) {
        return RequestConfig.custom()
                .setConnectTimeout(requestOptions.getConnectTimeout())
                .setSocketTimeout(requestOptions.getSocketTimeout())
                .setConnectionRequestTimeout(
                        requestOptions.getConnectionRequestTimeout())
                .setRedirectsEnabled(requestOptions.isRedirectsEnabled())
                .build();
    }

    /**
     * http请求
     *
     * @param method    请求方法
     * @param url       请求地址
     * @param urlParams url参数
     * @param bodyParam body参数,如果不是HttpEntity类型,使用json传递body参数
     * @param headers   请求头
     */
    public CloseableHttpResponse httpRequest(String method, String url, Object urlParams, Object bodyParam,
                                             Header... headers) {
        return this.httpRequest(method, url, urlParams, bodyParam, false, headers);
    }

    /**
     * get请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse getRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpGet.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * get请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse getRequest(String url, Object urlParams, Header... headers) {
        return this.getRequest(url, urlParams, null, headers);
    }

    /**
     * get请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse getRequest(String url) {
        return this.getRequest(url, null);
    }

    /**
     * delete请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse deleteRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpDelete.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * delete请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse deleteRequest(String url, Object urlParams, Header... headers) {
        return this.deleteRequest(url, urlParams, null, headers);
    }

    /**
     * delete请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse deleteRequest(String url) {
        return this.deleteRequest(url, null);
    }

    /**
     * head请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse headRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpHead.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * head请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse headRequest(String url, Object urlParams, Header... headers) {
        return this.headRequest(url, urlParams, null, headers);
    }

    /**
     * head请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse headRequest(String url) {
        return this.headRequest(url, null);
    }

    /**
     * options请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse optionsRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpOptions.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * options请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse optionsRequest(String url, Object urlParams, Header... headers) {
        return this.optionsRequest(url, urlParams, null, headers);
    }

    /**
     * options请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse optionsRequest(String url) {
        return this.optionsRequest(url, null);
    }

    /**
     * post请求
     *
     * @param url       请求地址, 不可为null
     * @param urlParams url参数, 可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数,
     *                  可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse postRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpPost.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * post请求
     *
     * @param url       请求地址, 不可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数,
     *                  可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse postRequest(String url, Object bodyParam, Header... headers) {
        return this.postRequest(url, null, bodyParam, headers);
    }

    /**
     * put请求
     *
     * @param url       请求地址, 不可为null
     * @param urlParams url参数, 可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数,
     *                  可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse putRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpPut.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * put请求
     *
     * @param url       请求地址, 不可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数,
     *                  可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse putRequest(String url, Object bodyParam, Header... headers) {
        return this.putRequest(url, null, bodyParam, headers);
    }

    /**
     * patch请求
     *
     * @param url       请求地址, 不可为null
     * @param urlParams url参数, 可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数,
     *                  可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse patchRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpPatch.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * patch请求
     *
     * @param url       请求地址, 不可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数,
     *                  可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse patchRequest(String url, Object bodyParam, Header... headers) {
        return this.patchRequest(url, null, bodyParam, headers);
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
