package org.linuxprobe.luava.http;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.linuxprobe.luava.json.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
     * get请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse getRequest(String url, Header... headers) {
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpGet httpGet = new HttpGet(url);
        if (headers != null && headers.length != 0) {
            httpGet.setHeaders(headers);
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpGet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * get请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse getRequest(String url) {
        return this.getRequest(url, (Header[]) null);
    }

    /**
     * get请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse getRequest(String url, Object urlParams, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.indexOf("?") != -1) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        return this.getRequest(url, headers);
    }

    /**
     * get请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse getRequest(String url, Object urlParams) {
        return this.getRequest(url, urlParams, (Header[]) null);
    }

    /**
     * delete请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse deleteRequest(String url, Header... headers) {
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpDelete httpDelete = new HttpDelete(url);
        if (headers != null && headers.length != 0) {
            httpDelete.setHeaders(headers);
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpDelete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * delete请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse deleteRequest(String url) {
        return this.deleteRequest(url, (Header[]) null);
    }

    /**
     * delete请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse deleteRequest(String url, Object urlParams, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.indexOf("?") != -1) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        return this.deleteRequest(url, headers);
    }

    /**
     * delete请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse deleteRequest(String url, Object urlParams) {
        return this.deleteRequest(url, urlParams, (Header[]) null);
    }

    /**
     * head请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse headRequest(String url, Header... headers) {
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpHead httpHead = new HttpHead(url);
        if (headers != null && headers.length != 0) {
            httpHead.setHeaders(headers);
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpHead);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * head请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse headRequest(String url) {
        return this.headRequest(url, (Header[]) null);
    }

    /**
     * head请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse headRequest(String url, Object urlParams, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.indexOf("?") != -1) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        return this.headRequest(url, headers);
    }

    /**
     * head请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse headRequest(String url, Object urlParams) {
        return this.headRequest(url, urlParams, (Header[]) null);
    }

    /**
     * options请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse optionsRequest(String url, Header... headers) {
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpOptions httpOptions = new HttpOptions(url);
        if (headers != null && headers.length != 0) {
            httpOptions.setHeaders(headers);
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpOptions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * options请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse optionsRequest(String url) {
        return this.optionsRequest(url, (Header[]) null);
    }

    /**
     * options请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse optionsRequest(String url, Object urlParams, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.indexOf("?") != -1) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        return this.optionsRequest(url, headers);
    }

    /**
     * options请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse optionsRequest(String url, Object urlParams) {
        return this.optionsRequest(url, urlParams, (Header[]) null);
    }

    /**
     * post请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     * @param headers   请求头
     */
    public CloseableHttpResponse postRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.indexOf("?") != -1) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpPost httpPost = new HttpPost(url);
        if (headers != null && headers.length != 0) {
            httpPost.setHeaders(headers);
        }
        if (bodyParam != null) {
            if (bodyParam instanceof HttpEntity) {
                if (logger.isTraceEnabled()) {
                    logger.trace("本次请求body参数:" + bodyParam);
                }
                httpPost.setEntity((HttpEntity) bodyParam);
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
                httpPost.setEntity(new StringEntity(bodyStr, ContentType.APPLICATION_JSON));
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpPost);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * post请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     */
    public CloseableHttpResponse postRequest(String url, Object urlParams, Object bodyParam) {
        return this.postRequest(url, urlParams, bodyParam, (Header[]) null);
    }

    /**
     * post请求
     *
     * @param url       请求地址
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     * @param headers   请求头
     */
    public CloseableHttpResponse postRequest(String url, Object bodyParam, Header... headers) {
        return this.postRequest(url, null, bodyParam, headers);
    }

    /**
     * post请求
     *
     * @param url       请求地址
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     */
    public CloseableHttpResponse postRequest(String url, Object bodyParam) {
        return this.postRequest(url, null, bodyParam);
    }

    /**
     * post请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse postRequest(Object urlParams, String url, Header... headers) {
        return this.postRequest(url, urlParams, null, headers);
    }

    /**
     * post请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse postRequest(Object urlParams, String url) {
        return this.postRequest(url, urlParams, null, (Header[]) null);
    }

    /**
     * post请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse postRequest(String url, Header... headers) {
        return this.postRequest(url, null, null, headers);
    }

    /**
     * post请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse postRequest(String url) {
        return this.postRequest(url, null, null, (Header[]) null);
    }

    /**
     * put请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     * @param headers   请求头
     */
    public CloseableHttpResponse putRequest(String url, Object urlParams, Object bodyParam, Header... headers) {
        if (urlParams != null) {
            String urlParam = Qs.stringify(urlParams);
            if (url.indexOf("?") != -1) {
                url += "&" + urlParam;
            } else {
                url += "?" + urlParam;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("本次请求地址:" + url);
        }
        CloseableHttpClient httpclient = this.getHttpClient();
        HttpPut httpPut = new HttpPut(url);
        if (headers != null && headers.length != 0) {
            httpPut.setHeaders(headers);
        }
        if (bodyParam != null) {
            if (bodyParam instanceof HttpEntity) {
                if (logger.isTraceEnabled()) {
                    logger.trace("本次请求body参数:" + bodyParam);
                }
                httpPut.setEntity((HttpEntity) bodyParam);
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
                httpPut.setEntity(new StringEntity(bodyStr, ContentType.APPLICATION_JSON));
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpPut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * put请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     */
    public CloseableHttpResponse putRequest(String url, Object urlParams, Object bodyParam) {
        return this.putRequest(url, urlParams, bodyParam, (Header[]) null);
    }

    /**
     * put请求
     *
     * @param url       请求地址
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     * @param headers   请求头
     */
    public CloseableHttpResponse putRequest(String url, Object bodyParam, Header... headers) {
        return this.putRequest(url, null, bodyParam, headers);
    }

    /**
     * put请求
     *
     * @param url       请求地址
     * @param bodyParam body参数,如果不是HttpEntity类型,则把bodyParam转换为json(如果是Sting,不转换),使用StringEntity传递参数
     */
    public CloseableHttpResponse putRequest(String url, Object bodyParam) {
        return this.putRequest(url, null, bodyParam);
    }

    /**
     * put请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     * @param headers   请求头
     */
    public CloseableHttpResponse putRequest(Object urlParams, String url, Header... headers) {
        return this.putRequest(url, urlParams, null, headers);
    }

    /**
     * put请求
     *
     * @param url       请求地址
     * @param urlParams url参数
     */
    public CloseableHttpResponse putRequest(Object urlParams, String url) {
        return this.putRequest(url, urlParams, null, (Header[]) null);
    }

    /**
     * put请求
     *
     * @param url     请求地址
     * @param headers 请求头
     */
    public CloseableHttpResponse putRequest(String url, Header... headers) {
        return this.putRequest(url, null, null, headers);
    }

    /**
     * put请求
     *
     * @param url 请求地址
     */
    public CloseableHttpResponse putRequest(String url) {
        return this.putRequest(url, null, null, (Header[]) null);
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
        }
        T result = null;
        result = JacksonUtils.conversion(entityString, type);
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 创建重试策略
     */
    private HttpRequestRetryHandler createHttpRequestRetryHandler() {
        /** 自定义重试策略 */
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                /** 如果已经重试了3次，就放弃 */
                if (executionCount >= 3) {
                    return false;
                }
                /** 如果服务器丢掉了连接，那么就重试 */
                else if (exception instanceof NoHttpResponseException) {
                    return true;
                }
                /** 不要重试SSL握手异常 */
                else if (exception instanceof SSLHandshakeException) {
                    return false;
                }
                /** io中断 */
                else if (exception instanceof InterruptedIOException) {
                    return false;
                }
                /** 目标服务器不可达 */
                else if (exception instanceof UnknownHostException) {
                    return false;
                }
                /** 连接被拒绝 */
                else if (exception instanceof ConnectTimeoutException) {
                    return false;
                }
                /** SSL握手异常 */
                else if (exception instanceof SSLException) {
                    return false;
                }
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                /**
                 * Retry if the request is considered idempotent
                 * 如果请求类型不是HttpEntityEnclosingRequest，被认为是幂等的，那么就重试
                 * HttpEntityEnclosingRequest指的是有请求体的request，比HttpRequest多一个Entity属性
                 * 而常用的GET请求是没有请求体的，POST、PUT都是有请求体的 Rest一般用GET请求获取数据，故幂等，POST用于新增数据，故不幂等
                 */
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
        return httpRequestRetryHandler;
    }

    /**
     * 创建连接池管理
     */
    private HttpClientConnectionManager createClientConnectionManager() {
        PoolingHttpClientConnectionManager clientConnectionManager = null;
        /** 这里设置信任所有证书 */
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                /** 信任所有 */
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslsf).register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
        /** 配置连接池 */
        clientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        /** 最大连接, */
        clientConnectionManager.setMaxTotal(this.connectPool.getAllMaxActive());
        /** 默认的每个路由的最大连接数 */
        clientConnectionManager.setDefaultMaxPerRoute(this.connectPool.getSingleMaxActive());
        /** 设置路由的最大连接数，会覆盖defaultMaxPerRoute */
        // clientConnectionManager.setMaxPerRoute(new HttpRoute(new
        // HttpHost("https://www.linuxprobe.org", 443)), 10);
        SocketConfig socketConfig = SocketConfig.custom()
                /** 是否立即发送数据，设置为true会关闭Socket缓冲，默认为false */
                .setTcpNoDelay(true)
                /** 是否可以在一个进程关闭Socket后，即使它还没有释放端口，其它进程还可以立即重用端口 */
                .setSoReuseAddress(true)
                /** 接收数据的等待超时时间，单位ms */
                .setSoTimeout(this.connectPool.getSocketTimeout())
                /** 关闭Socket时，要么发送完所有数据，要么等待60s后，就关闭连接，此时socket.close()是阻塞的 */
                .setSoLinger(6)
                /** 开启监视TCP连接是否有效 */
                .setSoKeepAlive(true).build();
        /** 默认路由的socket配置 */
        clientConnectionManager.setDefaultSocketConfig(socketConfig);
        /** 针对特定路由的socket配置 */
        /**
         * clientConnectionManager.setSocketConfig(new HttpHost("somehost", 80),
         * socketConfig);
         */
        return clientConnectionManager;
    }

    /**
     * 创建Http请求配置参数
     */
    private RequestConfig createRequestConfig() {
        RequestConfig requestConfig = RequestConfig.custom()
                // 连接超时时间
                .setConnectTimeout(this.connectPool.getConnectTimeout())
                // 读超时时间（等待数据超时时间
                .setSocketTimeout(this.connectPool.getSocketTimeout())
                // 从池中获取连接超时时间
                .setConnectionRequestTimeout(this.connectPool.getConnectionRequestTimeout()).build();
        return requestConfig;
    }

    public CloseableHttpClient getHttpClient() {
        if (this.connectPool == null) {
            return HttpClients.createDefault();
        } else {
            if (this.httpClient == null) {
                synchronized (this) {
                    if (this.httpClient == null) {
                        HttpClientConnectionManager clientConnectionManager = this.createClientConnectionManager();
                        this.httpClient = HttpClients.custom()
                                // 配置连接池管理对象
                                .setConnectionManager(clientConnectionManager)
                                // 默认请求配置
                                .setDefaultRequestConfig(this.createRequestConfig())
                                // 重试策略
                                .setRetryHandler(this.createHttpRequestRetryHandler()).build();
                        IdleConnectionEvictor idleConnectionEvictor = new IdleConnectionEvictor(clientConnectionManager, this.connectPool.getMaxLifetime(), this.connectPool.getCleanSleepTimeMs());
                        idleConnectionEvictor.start();
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
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() {
        this.close();
    }

    /**
     * 连接池
     */
    @Setter
    @Getter
    @Accessors(chain = true)
    public static class ConnectPool {
        private static final int defaultConnectTimeout = 5000;
        private static final int defaultSocketTimeout = 10000;
        private static final int defaultConnectionRequestTimeout = 20000;
        private static final int defaultSingleMaxActive = 5;
        private static final int defaultAllMaxActive = 40;
        /**
         * 连接池默认清理空闲30分钟的连接
         */
        private static final long defaultMaxLifetime = 1800000;
        /**
         * 连接池默认清理间隔时间30秒
         */
        private static final long defaultCleanSleepTimeMs = 30000;
        /**
         * 连接超时时间,单位毫秒,默认5秒
         */
        private int connectTimeout = defaultConnectTimeout;
        /**
         * 读取超时时间,单位毫秒,默认10秒
         */
        private int socketTimeout = defaultSocketTimeout;
        /**
         * 从池中获取连接超时时间,单位毫秒,默认20秒
         */
        private int connectionRequestTimeout = defaultConnectionRequestTimeout;
        /**
         * 单个路由最大活跃连接数,默认5
         */
        private int singleMaxActive = defaultSingleMaxActive;
        /**
         * 所有路由最大总活跃连接数,默认40,在队列服务器和主题服务器是分开的情况下,建议配置为singleMaxActive的两倍,如果
         * 不是分开的,建议与singleMaxActive保持一致
         */
        private int allMaxActive = defaultAllMaxActive;
        /**
         * 连最多高存活时间, 单位毫秒, 默认30分钟
         */
        private long maxLifetime = defaultMaxLifetime;
        /**
         * 连接池默认清理间隔时间30秒
         */
        private long cleanSleepTimeMs = defaultCleanSleepTimeMs;
    }
}
