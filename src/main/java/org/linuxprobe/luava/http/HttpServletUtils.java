package org.linuxprobe.luava.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	/**
	 * 对文件名进行编码，防止下载文件名称乱码
	 * 
	 * @param fileName 编码前文件名
	 * @return 返回编码后的文件名
	 */
	public static String encodeFileName(String fileName) {
		try {
			return URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 设置返回文件的文件名, 会自动编码文件名防止乱码
	 * 
	 * @param response HttpServletResponse
	 * @param fileName 编码前文件名
	 */
	public static void setResponseFileName(HttpServletResponse response, String fileName) {
		StringBuilder contentDispositionValue = new StringBuilder("attachment;");
		contentDispositionValue.append(" filename=" + encodeFileName(fileName) + ";");
		contentDispositionValue.append(" filename*=utf-8''" + encodeFileName(fileName));
		response.setHeader("Content-Disposition", contentDispositionValue.toString());
	}

	/**
	 * 返回文件, 会自动编码文件名, 防止乱码
	 * 
	 * @param response httpServletResponse
	 * @param file     返回文件
	 */
	public static void responseFile(HttpServletResponse response, File file) {
		try {
			ServletOutputStream out = response.getOutputStream();
			FileInputStream input = new FileInputStream(file);
			/** 设置文件ContentType类型，这样设置，会自动判断下载文件类型 */
			response.setContentType("multipart/form-data");
			String fileName = file.getName();
			setResponseFileName(response, fileName);
			byte[] bin = new byte[1024 * 4];
			for (int i = 0; i != -1;) {
				i = input.read(bin);
				if (i != -1)
					out.write(bin, 0, i);
				else {
					input.close();
					out.close();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 返回文件, 会自动编码文件名, 防止乱码
	 * 
	 * @param response    httpServletResponse
	 * @param fileName    文件名
	 * @param inputStream 输入流
	 */
	public static void responseFile(HttpServletResponse response, String fileName, InputStream inputStream) {
		try {
			ServletOutputStream out = response.getOutputStream();
			/** 设置文件ContentType类型，这样设置，会自动判断下载文件类型 */
			response.setContentType("multipart/form-data");
			setResponseFileName(response, fileName);
			byte[] bin = new byte[1024 * 4];
			for (int i = 0; i != -1;) {
				i = inputStream.read(bin);
				if (i != -1)
					out.write(bin, 0, i);
				else {
					out.close();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
