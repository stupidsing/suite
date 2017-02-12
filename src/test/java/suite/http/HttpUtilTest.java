package suite.http;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import suite.http.HttpUtil.HttpResult;
import suite.primitive.BytesUtil;
import suite.streamlet.Outlet;
import suite.util.To;

public class HttpUtilTest {

	@Test
	public void test() throws IOException {
		HttpResult result = HttpUtil.http("GET", new URL("http://feu.no-ip.info/"), To.source("{\"key\": \"value\"}"));
		System.out.println(result.responseCode);
		BytesUtil.copy(Outlet.from(result.out), System.out);
	}

}
