package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import suite.concurrent.Backoff;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;

public class HttpUtil {

	// keep timestamps to avoid overloading servers
	private static ConcurrentHashMap<String, AtomicLong> timestamps = new ConcurrentHashMap<>();

	public static class HttpResult {
		public int responseCode;
		public Outlet<Bytes> out;

		private HttpResult(int responseCode, Outlet<Bytes> out) {
			this.responseCode = responseCode;
			this.out = out;
		}
	}

	public static HttpResult http(URL url) {
		return http("GET", url);
	}

	public static HttpResult http(String method, URL url) {
		return http(method, url, Outlet.empty());
	}

	public static HttpResult http(String method, URL url, Outlet<Bytes> in) {
		return http(method, url, in, Collections.emptyMap());
	}

	public static HttpResult http(String method, URL url, Map<String, String> headers) {
		return http(method, url, Outlet.empty(), headers);
	}

	public static HttpResult http(String method, URL url, Outlet<Bytes> in, Map<String, String> headers) {
		AtomicLong al = timestamps.computeIfAbsent(url.getHost(), server -> new AtomicLong());
		Backoff backoff = new Backoff();
		long current, last, start, next;

		do {
			last = al.get();
			current = System.currentTimeMillis();
			start = Math.max(last, current);
			next = start + 2000;
		} while (!al.compareAndSet(last, next) || backoff.exponentially());

		Util.sleepQuietly(start - current);

		return Rethrow.ex(() -> {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod(method);

			headers.entrySet().forEach(e -> conn.setRequestProperty(e.getKey(), e.getValue()));

			try (OutputStream os = conn.getOutputStream()) {
				BytesUtil.copy(in, os);
			}

			int responseCode = conn.getResponseCode();
			if (responseCode == 200)
				return new HttpResult(responseCode, Read.bytes(conn.getInputStream()));
			else
				throw new IOException("HTTP returned " + responseCode + ":" + url);
		});
	}

}
