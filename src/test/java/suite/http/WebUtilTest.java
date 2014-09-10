package suite.http;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import suite.http.WebUtil.HttpResult;
import suite.primitive.BytesUtil;

public class WebUtilTest {

	@Test
	public void test() throws IOException {
		HttpResult result = WebUtil.http("GET", new URL("http://stupidsing.no-ip.org/"), BytesUtil.source("{\"key\": \"value\"}"));
		System.out.println(result.responseCode);
		BytesUtil.sink(result.out, System.out);
	}

}
