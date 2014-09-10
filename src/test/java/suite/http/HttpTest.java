package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.junit.Test;

import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.To;

public class HttpTest {

	private class HttpResult {
		private int responseCode;
		private Source<Bytes> out;

		private HttpResult(int responseCode, Source<Bytes> out) {
			this.responseCode = responseCode;
			this.out = out;
		}
	}

	@Test
	public void test() throws IOException {
		HttpResult result = http("GET" //
				, new URL("http://stupidsing.no-ip.org/") //
				, To.source(Arrays.asList(Bytes.of("{\"key\": \"value\"}".getBytes(FileUtil.charset)))));
		System.out.println(result.responseCode);
		BytesUtil.sink(result.out, System.out);
	}

	private HttpResult http(String method, URL url, Source<Bytes> in) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(method);

		try (OutputStream os = conn.getOutputStream()) {
			BytesUtil.sink(in, os);
		}

		return new HttpResult(conn.getResponseCode(), BytesUtil.source(conn.getInputStream()));
	}

}
