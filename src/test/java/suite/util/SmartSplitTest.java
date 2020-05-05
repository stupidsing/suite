package suite.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import primal.parser.Operator.Assoc;

public class SmartSplitTest {

	private SmartSplit ss = new SmartSplit();

	@Test
	public void test() {
		var s = "a,(b,c), d,'e,f',g";

		var expected = List.of( //
				"a", //
				"(b,c)", //
				" d", //
				"'e,f'", //
				"g");

		assertEquals(expected, ss.splitn(s, ",", Assoc.RIGHT).toList());
	}

}
