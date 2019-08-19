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

import java.io.IOException;

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
            if (url.contains("?")) {
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
            if (url.contains("?")) {
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
            if (url.contains("?")) {
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
            if (url.contains("?")) {
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
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() {
        this.close();
    }
}
