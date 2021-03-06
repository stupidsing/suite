package suite.http;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Pull;
import suite.primitive.Bytes_;

public class HttpUtilTest {

	@Test
	public void test() {
		var result = HttpClient //
				.get("https://pointless.online/") //
				.in(Pull.from("{\"key\": \"value\"}")) //
				.send();

		System.out.println(result.responseCode);
		Bytes_.copy(result.out, System.out::write);
	}

}
