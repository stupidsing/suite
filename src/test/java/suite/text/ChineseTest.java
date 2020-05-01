package suite.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChineseTest {

	private Chinese chinese = new Chinese();

	@Test
	public void test() {
		assertEquals("", chinese.cj("diu"));
		assertEquals("成", chinese.cj("ihs"));
		assertEquals("成", chinese.cj("ihs1"));
		assertEquals("成事在人", chinese.cjs("ihs jlln klg o "));
	}

}
