package suite.algo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.net.Bytes;
import suite.text.Segment;
import suite.util.FileUtil;
import suite.util.Pair;

public class LongestCommonContinuousSubsequenceTest {

	@Test
	public void test() {
		LongestCommonContinuousSubsequence lccs = new LongestCommonContinuousSubsequence();
		Pair<Segment, Segment> result = lccs.lccs(bytes("abczzzzz"), bytes("zzzzzabc"));

		assertEquals(3, result.t0.getStart());
		assertEquals(8, result.t0.getEnd());
		assertEquals(0, result.t1.getStart());
		assertEquals(5, result.t1.getEnd());
	}

	private Bytes bytes(String s) {
		return new Bytes(s.getBytes(FileUtil.charset));
	}

}
