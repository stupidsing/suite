package suite.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.node.io.Operator.Assoc;

public class ParseUtilTest {

	@Test
	public void test() {
		var s = "a,(b,c), d,'e,f',g";

		var expected = List.of( //
				"a", //
				"(b,c)", //
				" d", //
				"'e,f'", //
				"g");

		assertEquals(expected, ParseUtil.searchn(s, ",", Assoc.RIGHT).toList());
	}

}
