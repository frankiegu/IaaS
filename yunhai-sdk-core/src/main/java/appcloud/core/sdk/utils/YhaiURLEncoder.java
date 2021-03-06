package appcloud.core.sdk.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class YhaiURLEncoder {

	public final static String URL_ENCODING = "UTF-8";
	public static String encode(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, URL_ENCODING);
	}

	public static String percentEncode(String value) throws UnsupportedEncodingException {
		return null != value ? URLEncoder.encode(value, URL_ENCODING).replace("+", "%20")
				.replace("*", "%2A").replace("%7E", "~") : null;
	}
}
