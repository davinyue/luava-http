package org.rdlinux.luava.http;

/**
 * HTTP请求正文的客户端写出状态。
 */
public enum RequestWriteState {
    /**
     * 请求没有正文，或正文尚未开始写出。
     */
    NOT_STARTED,
    /**
     * 请求正文已经开始写出，但尚未确认完整写出。
     */
    STARTED,
    /**
     * 请求正文已经完整写入客户端输出流。
     */
    COMPLETED
}
