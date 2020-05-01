package suite.util;

import org.junit.jupiter.api.Test;
import suite.node.io.Operator.Assoc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
