package suite.http;

import org.junit.Test;

import suite.primitive.Bytes_;
import suite.util.To;

public class HttpUtilTest {

	@Test
	public void test() {
		var result = HttpUtil.http("GET", To.url("http://feu.no-ip.info/"), To.outlet("{\"key\": \"value\"}"));
		System.out.println(result.responseCode);
		Bytes_.copy(result.out, System.out::write);
	}

}
