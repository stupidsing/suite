package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChineseTest {

	@Test
	public void test() {
		assertEquals("成", new Chinese().cj("ihs"));
	}

}
