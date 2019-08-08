package suite.http;

import org.junit.Test;

import primal.Verbs.Pull;
import suite.primitive.Bytes_;

public class HttpUtilTest {

	@Test
	public void test() {
		var result = HttpUtil //
				.get("https://ywsing.onedse.com/") //
				.in(Pull.from("{\"key\": \"value\"}")) //
				.send();

		System.out.println(result.responseCode);
		Bytes_.copy(result.out, System.out::write);
	}

}
