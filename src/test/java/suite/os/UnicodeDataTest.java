package suite.os;

import org.junit.jupiter.api.Test;

public class UnicodeDataTest {

	@Test
	public void test() {
		System.out.println(new UnicodeData().getCharsOfClass("Lu"));
	}

}
