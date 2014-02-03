package suite.lcs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.text.Segment;
import suite.util.Pair;
import suite.util.To;

public class LccsTest {

	@Test
	public void test() {
		Lccs lccs = new Lccs();
		Pair<Segment, Segment> result = lccs.lccs(To.bytes("abczzzzz"), To.bytes("zzzzzabc"));

		assertEquals(3, result.t0.getStart());
		assertEquals(8, result.t0.getEnd());
		assertEquals(0, result.t1.getStart());
		assertEquals(5, result.t1.getEnd());
	}

}
