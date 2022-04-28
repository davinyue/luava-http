package org.rdlinux.luava.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.rdlinux.luava.json.JacksonUtils;
import org.slf4j.MDC;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

@Slf4j
public class HttpRequestUtils {
    private volatile CloseableHttpClient httpClient;
    private ConnectPool connectPool;

    /**
     * 构造一个使用连接池的http请求工具
     *
     * @param connectPool 连接池配置对象,当为null时,使用默认配置
     */
    public HttpRequestUtils(ConnectPool connectPool) {
        if (connectPool == null) {
            connectPool = new ConnectPool();
        }
        this.connectPool = connectPool;
    }

    /**
     * 构造一个不使用连接池的http请求工具
     */
    public HttpRequestUtils() {
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
     */
    public CloseableHttpResponse httpRequest(String method, String url, Object urlParams, Object bodyParam,
                                             boolean useFormData, Header... headers) {
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
        // 处理body参数
        if (bodyParam != null) {
            AbstractHttpEntity bodyEntity = null;
            if (bodyParam instanceof HttpEntity) {
                if (log.isTraceEnabled()) {
                    log.trace("本次请求body参数:" + bodyParam);
                }
                request.setEntity((HttpEntity) bodyParam);
            } else {
                if (useFormData) {
                    String stringify = Qs.stringify(bodyParam);
                    if (log.isTraceEnabled()) {
                        log.trace("本次请求body参数:" + stringify);
                    }
                    bodyEntity = new StringEntity(stringify, "UTF-8");
                    bodyEntity.setContentType("application/x-www-form-urlencoded");
                    bodyEntity.setContentEncoding("UTF-8");
                } else {
                    String bodyStr = null;
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
                request.setEntity(bodyEntity);
            }

        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
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

    public CloseableHttpClient getHttpClient() {
        if (this.connectPool == null) {
            return HttpClients.createDefault();
        } else {
            if (this.httpClient == null) {
                synchronized (this) {
                    if (this.httpClient == null) {
                        this.httpClient = new CloseableHttpClientBuilder().setConnectPool(this.connectPool).build();
                    }
                }
            }
            return this.httpClient;
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        try {
            if (this.httpClient != null) {
                this.httpClient.close();
                this.httpClient = null;
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    @Override
    protected void finalize() {
        this.close();
    }
}
