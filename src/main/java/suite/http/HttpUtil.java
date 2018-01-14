package suite.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import suite.concurrent.Backoff;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes_;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.ParseUtil;
import suite.util.Rethrow;
import suite.util.Thread_;
import suite.util.To;

public class HttpUtil {

	// keep timestamps to avoid overloading servers
	private static Map<String, AtomicLong> timestamps = new ConcurrentHashMap<>();

	public static class HttpResult {
		public int responseCode;
		public Outlet<Bytes> out;

		private HttpResult(int responseCode, Outlet<Bytes> out) {
			this.responseCode = responseCode;
			this.out = out;
		}
	}

	public static HttpResult get(URL url) {
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
		return http_(method, url, in, headers);
	}

	public static Map<String, URI> resolveLinks(URI uri) {
		String out = get(Rethrow.ex(() -> uri.toURL())).out.collect(As::utf8decode).map(Chars::toString).collect(As::joined);
		Map<String, URI> links = new HashMap<>();
		String[] m;
		while ((m = ParseUtil.fitCaseInsensitive(out, "<a", "href=\"", "\"", ">", "</a>")) != null) {
			String href = m[2];
			if (!href.startsWith("javascript:"))
				links.putIfAbsent(m[4], uri.resolve(href));
			out = m[5];
		}
		return links;
	}

	private static HttpResult http_(String method, URL url, Outlet<Bytes> in, Map<String, String> headers) {
		AtomicLong al = timestamps.computeIfAbsent(url.getHost(), server -> new AtomicLong());
		Backoff backoff = new Backoff();
		long current, last, start, next;

		do
			next = 3000l + (start = Math.max(last = al.get(), current = System.currentTimeMillis()));
		while (!al.compareAndSet(last, next) || backoff.exponentially());

		Thread_.sleepQuietly(start - current);

		return Rethrow.ex(() -> httpApache(method, url, in, headers));
		// return Rethrow.ex(() -> httpJre(method, url, in, headers));
	}

	private static HttpResult httpApache(String method, URL url, Outlet<Bytes> in, Map<String, String> headers) throws IOException {
		LogUtil.info("START " + method + " " + url);
		CloseableHttpClient client = HttpClients.createDefault();

		HttpRequestBase request = new HttpRequestBase() {
			{
				setURI(URI.create(url.toString()));
				headers.entrySet().forEach(e -> addHeader(e.getKey(), e.getValue()));
			}

			public String getMethod() {
				return method;
			}
		};

		CloseableHttpResponse response = client.execute(request);

		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();
		InputStream inputStream = response.getEntity().getContent();
		Outlet<Bytes> out = To.outlet(inputStream) //
				.closeAtEnd(inputStream) //
				.closeAtEnd(response) //
				.closeAtEnd(client) //
				.closeAtEnd(() -> LogUtil.info("END__ " + method + " " + url));

		if (statusCode == HttpURLConnection.HTTP_OK)
			return new HttpResult(statusCode, out);
		else
			throw new IOException("HTTP returned " + statusCode //
					+ ": " + url //
					+ ": " + statusLine.getReasonPhrase() //
					+ ": " + out.collect(As::string));
	}

	@SuppressWarnings("unused")
	private static HttpResult httpJre(String method, URL url, Outlet<Bytes> in, Map<String, String> headers) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(method);

		headers.entrySet().forEach(e -> conn.setRequestProperty(e.getKey(), e.getValue()));

		try (OutputStream os = conn.getOutputStream()) {
			Bytes_.copy(in, os::write);
		}

		int responseCode = conn.getResponseCode();
		Outlet<Bytes> out = To.outlet(conn.getInputStream());

		if (responseCode == HttpURLConnection.HTTP_MOVED_PERM //
				|| responseCode == HttpURLConnection.HTTP_MOVED_TEMP //
				|| responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
			String cookies1 = conn.getHeaderField("Set-Cookie");
			URL url1 = To.url(conn.getHeaderField("Location"));

			Map<String, String> headers1 = new HashMap<>(headers);
			if (cookies1 != null)
				headers1.put("Cookie", cookies1);

			return http(method, url1, in, headers1);
		} else if (responseCode == HttpURLConnection.HTTP_OK)
			return new HttpResult(responseCode, out);
		else
			throw new IOException("HTTP returned " + responseCode //
					+ ": " + url //
					+ ": " + conn.getResponseMessage() //
					+ ": " + out.collect(As::string));
	}

}
