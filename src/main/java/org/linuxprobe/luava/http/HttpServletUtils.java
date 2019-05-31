package org.linuxprobe.luava.http;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class HttpServletUtils {
	/** 判断是否是ajax请求 */
	public static boolean isAjax(HttpServletRequest request) {
		if (request.getMethod().equalsIgnoreCase("GET")) {
			if (request.getHeader("postman-token") != null
					|| request.getHeader("user-agent").toLowerCase().contains("postman")) {
				return true;
			}
			String XRequestedWithHeader = request.getHeader("X-Requested-With");
			if ("XMLHttpRequest".equalsIgnoreCase(XRequestedWithHeader)) {
				return true;
			}
			String contentTypeHeader = request.getHeader("Content-Type");
			if (StringUtils.isNotBlank(contentTypeHeader) && contentTypeHeader.contains("json")) {
				return true;
			}
			String acceptHeader = request.getHeader("Accept");
			if (StringUtils.isNotBlank(acceptHeader) && acceptHeader.contains("json")) {
				return true;
			}
			return false;
		} else {
			return true;
		}
	}

	/** 判断是否是IE浏览器 */
	public static boolean isMSBrowser(HttpServletRequest request) {
		String[] IEBrowserSignals = { "MSIE", "Trident", "Edge" };
		String userAgent = request.getHeader("User-Agent");
		if (userAgent == null) {
			return false;
		}
		for (String signal : IEBrowserSignals) {
			if (userAgent.contains(signal)) {
				return true;
			}
		}
		return false;
	}

	/** 根据名称获取cookie */
	public static Cookie getCookie(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookie.getName().equals(cookieName)) {
				return cookie;
			}
		}
		return null;
	}

	/** 根据名称获取cookie值 */
	public static String getCookieValue(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookie.getName().equals(cookieName)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static String getRemoteAddr(HttpServletRequest request) {
		String ipAddress = request.getHeader("x-forwarded-for");
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
			if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
				/** 根据网卡取本机配置的IP */
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				ipAddress = inet.getHostAddress();
			}
		}
		/** 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割 */
		if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length() = 15
			if (ipAddress.indexOf(",") > 0) {
				ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
			}
		}
		return ipAddress;
	}
}
