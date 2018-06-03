package suite.inspect;

import org.junit.Test;

public class DumpTest {

	private int i = 3;
	private String s = "Hello";

	@Test
	public void test() {
		Dump.details(this);
	}

	public int getInt() {
		return i;
	}

	public String getString() {
		return s;
	}

}
