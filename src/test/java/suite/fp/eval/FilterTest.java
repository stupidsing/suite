package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;

public class FilterTest {

	@Test
	public void testDirect() {
		assertEquals("abcdef", eval("c => c", "abcdef"));
	}

	@Test
	public void testMap() {
		assertEquals("bcdefg", eval("map {`+ 1`}", "abcdef"));
	}

	@Test
	public void testSplit() {
		assertEquals("abc\ndef\nghi", eval("tail . concat . map {cons {10}} . split {32}", "abc def ghi"));
	}

	private static String eval(String program, String in) {
		return Suite.evaluateFilterFun(program, true, in);
	}

}
