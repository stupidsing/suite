package suite.http;

import org.junit.Test;

import suite.primitive.Bytes_;
import suite.util.To;

public class HttpUtilTest {

	@Test
	public void test() {
		var result = HttpUtil //
				.get("http://feu.no-ip.info/") //
				.in(To.outlet("{\"key\": \"value\"}")) //
				.send();

		System.out.println(result.responseCode);
		Bytes_.copy(result.out, System.out::write);
	}

}
