package suite.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ChineseTest {

	@Test
	public void test() {
		assertEquals("成", new Chinese().cj("ihs"));
	}

}
