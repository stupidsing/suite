package suite.lcs;

import org.junit.jupiter.api.Test;
import suite.util.To;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LccsTest {

	@Test
	public void test() {
		var lccs = new Lccs();
		var result = lccs.lccs(To.bytes("abczzzzz"), To.bytes("zzzzzabc"));

		assertEquals(3, result.k.start);
		assertEquals(8, result.k.end);
		assertEquals(0, result.v.start);
		assertEquals(5, result.v.end);
	}

}
