package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.util.FunUtil.Source;

public class WebUtil {

	public static class HttpResult {
		public int responseCode;
		public Source<Bytes> out;

		private HttpResult(int responseCode, Source<Bytes> out) {
			this.responseCode = responseCode;
			this.out = out;
		}
	}

	public static HttpResult http(String method, URL url, Source<Bytes> in) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(method);

		try (OutputStream os = conn.getOutputStream()) {
			BytesUtil.sink(in, os);
		}

		int responseCode = conn.getResponseCode();
		if (responseCode == 200)
			return new HttpResult(responseCode, BytesUtil.source(conn.getInputStream()));
		else
			throw new IOException("HTTP returned " + responseCode + ":" + url);
	}

}
