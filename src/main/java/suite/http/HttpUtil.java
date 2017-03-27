package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.Outlet;
import suite.util.FunUtil;
import suite.util.Rethrow;
import suite.util.To;

public class HttpUtil {

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
		return http(method, url, Outlet.from(FunUtil.nullSource()));
	}

	public static HttpResult http(String method, URL url, Outlet<Bytes> in) {
		return http(method, url, in, Collections.emptyMap());
	}

	public static HttpResult http(String method, URL url, Outlet<Bytes> in, Map<String, String> headers) {
		return Rethrow.ioException(() -> {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod(method);

			headers.entrySet().forEach(e -> conn.setRequestProperty(e.getKey(), e.getValue()));

			try (OutputStream os = conn.getOutputStream()) {
				BytesUtil.copy(in, os);
			}

			int responseCode = conn.getResponseCode();
			if (responseCode == 200)
				return new HttpResult(responseCode, To.bytesOutlet(conn.getInputStream()));
			else
				throw new IOException("HTTP returned " + responseCode + ":" + url);
		});
	}

}
