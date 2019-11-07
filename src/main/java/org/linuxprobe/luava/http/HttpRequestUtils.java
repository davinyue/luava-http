package org.linuxprobe.luava.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.linuxprobe.luava.json.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;

public class HttpRequestUtils {
    private static Logger logger = LoggerFactory.getLogger(HttpRequestUtils.class);
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

    /**
     * http请求
     *
     * @param method    请求方法
     * @param url       请求地址
     * @param urlParams url参数
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     * @param headers   请求头
     */
    public CloseableHttpResponse httpRequest(String method, String url, Object urlParams, Object bodyParam, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.contains("?")) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
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
            request.setHeaders(headers);
        }
        //添加调用链追踪请求头
        if (request.getFirstHeader(SleuthConst.traceIdHeader) == null) {
            String traceId = MDC.get(SleuthConst.traceIdLogName);
            if (traceId != null) {
                request.addHeader(SleuthConst.traceIdHeader, traceId);
            }
        }
        if (request.getFirstHeader(SleuthConst.spanIdHeader) == null) {
            String spanId = MDC.get(SleuthConst.spanIdLogName);
            if (spanId != null) {
                request.addHeader(SleuthConst.spanIdHeader, spanId);
            }
        }
        if (request.getFirstHeader(SleuthConst.parentSpanIdHeader) == null) {
            String parentSpanId = MDC.get(SleuthConst.parentSpanIdLogName);
            if (parentSpanId != null) {
                request.addHeader(SleuthConst.parentSpanIdHeader, parentSpanId);
            }
        }
        if (bodyParam != null) {
            if (bodyParam instanceof HttpEntity) {
                if (logger.isTraceEnabled()) {
                    logger.trace("本次请求body参数:" + bodyParam);
                }
                request.setEntity((HttpEntity) bodyParam);
            } else {
                String bodyStr = null;
                if (bodyParam instanceof String) {
                    bodyStr = (String) bodyParam;
                } else if (bodyParam instanceof StringBuilder || bodyParam instanceof StringBuffer) {
                    bodyStr = bodyParam.toString();
                } else {
                    bodyStr = JacksonUtils.toJsonString(bodyParam);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("本次请求body参数:" + bodyStr);
                }
                request.setEntity(new StringEntity(bodyStr, ContentType.APPLICATION_JSON));
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
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数, 可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse postRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpPost.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * post请求
     *
     * @param url       请求地址, 不可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数, 可为null
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
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数, 可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse putRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpPut.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * put请求
     *
     * @param url       请求地址, 不可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数, 可为null
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
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数, 可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse patchRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        return this.httpRequest(HttpPatch.METHOD_NAME, url, urlParams, bodyParam, headers);
    }

    /**
     * patch请求
     *
     * @param url       请求地址, 不可为null
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数, 可为null
     * @param headers   请求头, 可为null
     */
    public CloseableHttpResponse patchRequest(String url, Object bodyParam, Header... headers) {
        return this.patchRequest(url, null, bodyParam, headers);
    }

    /**
     * 把返回数据转换为指定对象
     */
    public static <T> T responseDataConversion(HttpResponse response, Class<T> type) {
        if (response == null) {
            return null;
        }
        HttpEntity entity = response.getEntity();
        String entityString = null;
        try {
            entityString = EntityUtils.toString(entity);
        } catch (ParseException | IOException e1) {
            throw new RuntimeException(e1);
        } finally {
            try {
                EntityUtils.consume(entity);
            } catch (IOException ignored) {
            }
        }
        if (type == String.class) {
            return (T) entityString;
        }
        return JacksonUtils.conversion(entityString, type);
    }

    public CloseableHttpClient getHttpClient() {
        if (this.connectPool == null) {
            return HttpClients.createDefault();
        } else {
            if (this.httpClient == null) {
                synchronized (this) {
                    if (this.httpClient == null) {
                        this.httpClient = CloseableHttpClientBuilder.builder(this.connectPool);
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
            logger.error("", e);
        }
    }

    @Override
    protected void finalize() {
        this.close();
    }
}
