package suite.os;

import org.junit.Test;

import suite.os.UnicodeData;

public class UnicodeDataTest {

	@Test
	public void test() {
		System.out.println(new UnicodeData().getCharsOfClass("Lu"));
	}

}
