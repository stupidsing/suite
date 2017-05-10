package suite.http;

import org.junit.Test;

import suite.http.HttpUtil.HttpResult;
import suite.primitive.BytesUtil;
import suite.util.To;

public class HttpUtilTest {

	@Test
	public void test() {
		HttpResult result = HttpUtil.http("GET", To.url("http://feu.no-ip.info/"), To.outlet("{\"key\": \"value\"}"));
		System.out.println(result.responseCode);
		BytesUtil.copy(result.out, System.out);
	}

}
