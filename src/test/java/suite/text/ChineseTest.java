package suite.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ChineseTest {

	private Chinese chinese = new Chinese();

	@Test
	public void test() {
		assertEquals("成", chinese.cj("ihs"));
		assertEquals("成", chinese.cj("ihs1"));
		assertEquals("成事在人", chinese.cjs("ihs jlln klg o "));
	}

}
