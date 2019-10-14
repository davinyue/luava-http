package org.linuxprobe.luava.http;

public class SleuthConst {
    public static final String traceIdHeader = "x-b3-traceid";
    public static final String spanIdHeader = "x-b3-spanid";
    public static final String parentSpanIdHeader = "x-b3-parentspanid";
    public static final String parentSpanIdLogName = "x-b3-parentspanid";
    /**
     * 该请求头表示是否上报, 0表示不上报, 1表示上报
     */
    public static final String sampledHeader = "x-b3-sampled";
    public static final String traceIdLogName = "traceId";
    public static final String X_B3_TraceId_LogName = "X-B3-TraceId";
    public static final String spanIdLogName = "spanId";
    public static final String X_B3_SpanId_LogName = "X-B3-SpanId";
    public static final String spanExportableLogName = "spanExportable";
    public static final String X_Span_Export_LogName = "X-Span-Export";
}
