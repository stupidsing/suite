package suite.http;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import suite.http.WebUtil.HttpResult;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.Streamlet;
import suite.util.To;

public class WebUtilTest {

	@Test
	public void test() throws IOException {
		HttpResult result = WebUtil.http("GET", new URL("http://stupidsing.no-ip.org/"), To.source("{\"key\": \"value\"}"));
		System.out.println(result.responseCode);
		BytesUtil.copy(new Streamlet<Bytes>(result.out), System.out);
	}

}
